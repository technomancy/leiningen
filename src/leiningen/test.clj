(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:use [clojure.java.io :only [file]]
        [leiningen.core :only [*interactive?*]]
        [leiningen.util.ns :only [namespaces-in-dir]]
        [leiningen.compile :only [eval-in-project]])
  (:import (java.io File)))

;; TODO: switch to using *interactive* flag in 2.0.
(def ^{:dynamic true} *exit-after-tests* true)

(defn- form-for-hook-selectors [selectors]
  `(when (seq ~selectors)
     (leiningen.util.injected/add-hook
      (resolve 'clojure.test/test-var)
      (fn test-var-with-selector [test-var# var#]
        (when (reduce #(or %1 (%2 (assoc (meta var#) ::var var#)))
                      false ~selectors)
          (test-var# var#))))))

(defn form-for-testing-namespaces
  "Return a form that when eval'd in the context of the project will test
each namespace and print an overall summary."
  ([namespaces result-file & [selectors]]
     `(do
        (doseq [n# '~namespaces]
          (require n# :reload))
        ~(form-for-hook-selectors selectors)
        (let [failures# (atom #{})
              _# (leiningen.util.injected/add-hook
                  #'clojure.test/report
                  (fn report-with-failures [report# m# & args#]
                    (when (#{:error :fail} (:type m#))
                      (swap! failures# conj
                             (-> clojure.test/*testing-vars*
                                 first meta :ns ns-name)))
                    (apply report# m# args#)))
              summary# (binding [clojure.test/*test-out* *out*]
                         (apply ~'clojure.test/run-tests '~namespaces))]
          (spit ".lein-failures" (pr-str @failures#))
          ;; Stupid ant won't let us return anything, so write results to disk
          (with-open [w# (-> (java.io.File. ~result-file)
                             (java.io.FileOutputStream.)
                             (java.io.OutputStreamWriter.))]
            (.write w# (pr-str summary#)))
          (when (or ~*exit-after-tests* (not ~*interactive?*))
            (System/exit (+ (:error summary#) (:fail summary#))))))))

(defn- read-args [args project]
  (let [args (map read-string args)
        nses (if (or (empty? args) (every? keyword? args))
               (sort (namespaces-in-dir (:test-path project)))
               (filter symbol? args))
        selectors (map (merge {:all '(constantly true)}
                              (:test-selectors project)) (filter keyword? args))
        selectors (if (and (empty? selectors)
                           (:default (:test-selectors project)))
                    [(:default (:test-selectors project))]
                    selectors)]
    (when (and (not (:test-selectors project)) (some keyword? args))
      (throw (Exception. "Must specify :test-selectors in project.clj")))
    [nses selectors]))

(defn test
  "Run the project's tests.

Accepts either a list of test namespaces to run or a list of test
selectors. With no arguments, runs all tests."
  [project & tests]
  (when (:eval-in-leiningen project)
    (require '[clojure walk template stacktrace]))
  (let [[nses selectors] (read-args tests project)
        result (doto (File/createTempFile "lein" "result") .deleteOnExit)]
    (eval-in-project project (form-for-testing-namespaces
                              nses (.getAbsolutePath result) (vec selectors))
                     '(require 'clojure.test))
    (if (and (.exists result) (pos? (.length result)))
      (let [summary (read-string (slurp (.getAbsolutePath result)))
            success? (zero? (+ (:error summary) (:fail summary)))]
        (if success? 0 1))
      1)))
