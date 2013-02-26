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

(defn- form-for-hook-selectors [selectors]
  `(when (seq ~selectors)
     (leiningen.core.injected/add-hook
      (resolve 'clojure.test/test-var)
      (fn test-var-with-selector [test-var# var#]
        (when (reduce (fn [acc# [selector# args#]]
                        (let [sfn# (if (vector? selector#)
                                     (second selector#)
                                     selector#)]
                          (or acc#
                              (apply sfn#
                                     (merge (-> var# meta :ns meta)
                                            (assoc (meta var#) ::var var#))
                                     args#))))
                      false ~selectors)
          (test-var# var#))))))

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
          :when (reduce (fn [acc# [selector# args#]]
                          (or acc#
                              (apply (if (vector? selector#)
                                       (second selector#)
                                       selector#)
                                     (merge (-> var# meta :ns meta)
                                            (assoc (meta var#) ::var var#))
                                     args#)))
                        false ~selectors)]
      ns#)))

(defn form-for-testing-namespaces
  "Return a form that when eval'd in the context of the project will test
  each namespace and print an overall summary."
  ([namespaces _ & [selectors]]
     (let [ns-sym (gensym "namespaces")]
       `(let [~ns-sym ~(form-for-select-namespaces namespaces selectors)]
          (when (seq ~ns-sym)
            (apply require :reload ~ns-sym))
          ~(form-for-hook-selectors selectors)
          (let [failures# (atom #{})
                selected-namespaces#  ~(form-for-nses-selectors-match selectors ns-sym)
                _# (leiningen.core.injected/add-hook
                    #'clojure.test/report
                    (fn [report# m# & args#]
                      (when (#{:error :fail} (:type m#))
                        (let [first-var# (-> clojure.test/*testing-vars* first meta)]
                          (swap! failures# conj (ns-name (:ns first-var#)))
                          (println "\nlein test :only"
                                   (str (ns-name (:ns first-var#))
                                        "/"
                                        (:name first-var#)))))
                      (if (= :begin-test-ns (:type m#))
                        (clojure.test/with-test-out
                          (println "\nlein test" (ns-name (:ns m#))))
                        (apply report# m# args#))))
                summary# (binding [clojure.test/*test-out* *out*]
                           (apply ~'clojure.test/run-tests selected-namespaces#))]
            (spit ".lein-failures" (pr-str @failures#))
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
  (if (and (.endsWith possible-file ".clj") (.exists (io/file possible-file)))
    (str (b/ns-form-for-file possible-file))
    possible-file))

(defn- read-args [args project]
  (let [args  (->> args (map convert-to-ns) (map read-string))
        [nses given-selectors] (split-selectors args)
        nses (or (seq nses)
                 (sort
                  (b/namespaces-on-classpath
                   :classpath (map io/file (:test-paths project)))))
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
                     :integration :integration
                     :all (constantly true)}

Arguments to this task will be considered test selectors if they are keywords,
otherwise arguments must be test namespaces or files to run. With no
arguments the :default test selector is used if present, otherwise all
tests are run.

A default :only test-selector is available to run select tests. For example,
`lein test :only leiningen.test.test/test-default-selector` only runs the
specified test. A default :all test-selector is available to run all tests."
  [project & tests]
  (binding [main/*exit-process?* (if (= :leiningen (:eval-in project))
                                   false
                                   main/*exit-process?*)
            *exit-after-tests* (if (= :leiningen (:eval-in project))
                                   false
                                   *exit-after-tests*)]
    (let [project (project/merge-profiles project [:leiningen/test :test])
          [nses selectors] (read-args tests project)
          form (form-for-testing-namespaces nses nil (vec selectors))]
      (try (when-let [n (eval/eval-in-project project form
                                              '(require 'clojure.test))]
             (when (pos? n)
               (throw (ex-info "Tests Failed" {:exit-code n}))))
           (catch clojure.lang.ExceptionInfo e
             (main/abort "Tests failed."))))))