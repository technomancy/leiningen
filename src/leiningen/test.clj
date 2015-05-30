(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:require [clojure.java.io :as io]
            [bultitude.core :as b]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project])
  (:import (java.io File PushbackReader)))

(def ^:dynamic *exit-after-tests* true)

(def form-for-suppressing-unselected-tests
  "A function that figures out which vars need to be suppressed based on the
  given selectors, moves their :test metadata to :leiningen/skipped-test (so
  that clojure.test won't think they are tests), runs the given function, and
  then sets the metadata back."
  `(fn [namespaces# selectors# func#]
     (let [copy-meta# (fn [var# from-key# to-key#]
                        (if-let [x# (get (meta var#) from-key#)]
                          (alter-meta! var# #(-> % (assoc to-key# x#) (dissoc from-key#)))))
           vars# (if (seq selectors#)
                   (->> namespaces#
                        (mapcat (comp vals ns-interns))
                        (remove (fn [var#]
                                  (some (fn [[selector# args#]]
                                          (let [sfn# (if (vector? selector#)
                                                       (second selector#)
                                                       selector#)]
                                            (apply sfn#
                                                   (merge (-> var# meta :ns meta)
                                                          (assoc (meta var#) ::var var#))
                                                   args#)))
                                        selectors#)))))
           copy# #(doseq [v# vars#] (copy-meta# v# %1 %2))]
       (copy# :test :leiningen/skipped-test)
       (try (func#)
            (finally
              (copy# :leiningen/skipped-test :test))))))

(defn- form-for-select-namespaces [namespaces selectors]
  `(reduce (fn [acc# [f# args#]]
             (if (vector? f#)
               (filter #(apply (first f#) % args#) acc#)
               acc#))
           '~namespaces ~selectors))

(defn- form-for-nses-selectors-match [selectors ns-sym]
  `(distinct
    (for [ns# ~ns-sym
          [_# var#] (ns-publics ns#)
          :when (some (fn [[selector# args#]]

                        (apply (if (vector? selector#)
                                 (second selector#)
                                 selector#)
                               (merge (-> var# meta :ns meta)
                                      (assoc (meta var#) ::var var#))
                               args#))
                      ~selectors)]
      ns#)))

;; TODO: make this an arg to form-for-testing-namespaces in 3.0.
(def ^:private ^:dynamic *monkeypatch?* true)

(defn form-for-testing-namespaces
  "Return a form that when eval'd in the context of the project will test each
  namespace and print an overall summary."
  ([namespaces _ & [selectors]]
     (let [ns-sym (gensym "namespaces")]
       `(let [~ns-sym ~(form-for-select-namespaces namespaces selectors)]
          (when (seq ~ns-sym)
            (apply require :reload ~ns-sym))
          (let [failures# (atom #{})
                selected-namespaces# ~(form-for-nses-selectors-match selectors ns-sym)
                _# (when ~*monkeypatch?*
                     (leiningen.core.injected/add-hook
                      #'clojure.test/report
                      (fn [report# m# & args#]
                        (when (#{:error :fail} (:type m#))
                          (when-let [first-var# (-> clojure.test/*testing-vars* first meta)]
                            (swap! failures# conj (ns-name (:ns first-var#)))
                            (newline)
                            (println "lein test :only"
                                     (str (ns-name (:ns first-var#)) "/"
                                          (:name first-var#)))))
                        (if (= :begin-test-ns (:type m#))
                          (clojure.test/with-test-out
                            (newline)
                            (println "lein test" (ns-name (:ns m#))))
                          (apply report# m# args#)))))
                summary# (binding [clojure.test/*test-out* *out*]
                           (~form-for-suppressing-unselected-tests
                            selected-namespaces# ~selectors
                            #(apply ~'clojure.test/run-tests selected-namespaces#)))]
            (spit ".lein-failures" (if ~*monkeypatch?*
                                     (pr-str @failures#)
                                     "#<disabled :monkeypatch-clojure-test>"))
            (if ~*exit-after-tests*
              (System/exit (+ (:error summary#) (:fail summary#)))
              (+ (:error summary#) (:fail summary#))))))))

(defn- split-selectors [args]
  (let [[nses selectors] (split-with (complement keyword?) args)]
    [nses
     (loop [acc {} [selector & selectors] selectors]
       (if (seq selectors)
         (let [[args next] (split-with (complement keyword?) selectors)]
           (recur (assoc acc selector (list 'quote args))
                  next))
         (if selector
           (assoc acc selector ())
           acc)))]))

(defn- partial-selectors [project-selectors selectors]
  (for [[k v] selectors
        :let [selector-form (k project-selectors)]
        :when selector-form]
    [selector-form v]))

(def ^:private only-form
  ['(fn [ns & vars]
      ((set (for [v vars]
              (-> (str v)
                  (.split "/")
                  (first)
                  (symbol))))
       ns))
   '(fn [m & vars]
      (some #(let [var (str "#'" %)]
               (if (some #{\/} var)
                 (= var (-> m ::var str))
                 (= % (ns-name (:ns m)))))
            vars))])

(defn- convert-to-ns [possible-file]
  (if (and (re-matches #".*\.cljc?" possible-file) (.exists (io/file possible-file)))
    (str (second (b/ns-form-for-file possible-file)))
    possible-file))

(defn ^:internal read-args [args project]
  (let [args (->> args (map convert-to-ns) (map read-string))
        [nses given-selectors] (split-selectors args)
        nses (or (seq nses)
                 (sort (b/namespaces-on-classpath
                        :classpath (map io/file (distinct (:test-paths project)))
                        :ignore-unreadable? false)))
        selectors (partial-selectors (merge {:all '(constantly true)}
                                            {:only only-form}
                                            (:test-selectors project))
                                     given-selectors)
        selectors (if (and (empty? selectors)
                           (:default (:test-selectors project)))
                    [[(:default (:test-selectors project)) ()]]
                    selectors)]
    (when (and (empty? selectors)
               (seq given-selectors))
      (main/abort "Please specify :test-selectors in project.clj"))
    [nses selectors]))

(defn test
  "Run the project's tests.

Marking deftest or ns forms with metadata allows you to pick selectors to
specify a subset of your test suite to run:

    (deftest ^:integration network-heavy-test
      (is (= [1 2 3] (:numbers (network-operation)))))

Write the selectors in project.clj:

    :test-selectors {:default (complement :integration)
                     :integration :integration}

Arguments to this task will be considered test selectors if they are keywords,
otherwise arguments must be test namespaces or files to run. With no
arguments the :default test selector is used if present, otherwise all
tests are run. Test selector arguments must come after the list of namespaces.

A default :only test-selector is available to run select tests. For example,
`lein test :only leiningen.test.test/test-default-selector` only runs the
specified test. A default :all test-selector is available to run all tests."
  [project & tests]
  (binding [main/*exit-process?* (if (= :leiningen (:eval-in project))
                                   false
                                   main/*exit-process?*)
            *exit-after-tests* (if (= :leiningen (:eval-in project))
                                 false
                                 *exit-after-tests*)
            *monkeypatch?* (:monkeypatch-clojure-test project true)]
    (let [project (project/merge-profiles project [:leiningen/test :test])
          [nses selectors] (read-args tests project)
          form (form-for-testing-namespaces nses nil (vec selectors))]
      (try (when-let [n (eval/eval-in-project project form
                                              '(require 'clojure.test))]
             (when (and (number? n) (pos? n))
               (throw (ex-info "Tests Failed" {:exit-code n}))))
           (catch clojure.lang.ExceptionInfo e
             (main/abort "Tests failed."))))))
