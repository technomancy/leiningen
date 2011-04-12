(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:use [clojure.java.io :only [file]]
        [leiningen.util.ns :only [namespaces-in-dir]]
        [leiningen.compile :only [eval-in-project]])
  (:import (java.io File)))

(def *exit-after-tests* true)

(defn- form-for-hook-selectors [selectors]
  `(when (seq ~selectors)
     (if-let [add-hook# (resolve 'robert.hooke/add-hook)]
       (add-hook# (resolve 'clojure.test/test-var)
                  (fn test-var-with-selector [test-var# var#]
                    (when (reduce #(or %1 (%2 (assoc (meta var#) ::var var#)))
                                  false ~selectors)
                      (test-var# var#))))
       (throw (Exception. "Test selectors require robert/hooke dep.")))))

(defn form-for-testing-namespaces
  "Return a form that when eval'd in the context of the project will test
each namespace and print an overall summary."
  ([namespaces result-file & [selectors]]
     `(do
        (doseq [n# '~namespaces]
          (require n# :reload))
        ~(form-for-hook-selectors selectors)
        (let [summary# (binding [clojure.test/*test-out* *out*]
                         (apply ~'clojure.test/run-tests '~namespaces))]
          (when-not (= "1.5" (System/getProperty "java.specification.version"))
            (shutdown-agents))
          ;; Stupid ant won't let us return anything, so write results to disk
          (with-open [w# (-> (java.io.File. ~result-file)
                             (java.io.FileOutputStream.)
                             (java.io.OutputStreamWriter.))]
            (.write w# (pr-str summary#)))
          (when ~*exit-after-tests*
            (System/exit 0))))))

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
                     nil nil `(do (require '~'clojure.test)
                                  ~(when (seq selectors)
                                     '(require 'robert.hooke))))
    (if (and (.exists result) (pos? (.length result)))
      (let [summary (read-string (slurp (.getAbsolutePath result)))
            success? (zero? (+ (:error summary) (:fail summary)))]
        (if success? 0 1))
      1)))
