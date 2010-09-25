(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:use [clojure.java.io :only [file]]
        [leiningen.util.ns :only [namespaces-in-dir]]
        [leiningen.compile :only [eval-in-project]])
  (:import [java.io File]))

(defn- init-args [java & files]
  (doseq [f files]
    (.setValue (.createArg java) "-i")
    (.setValue (.createArg java) f)))

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
          (require n#))
        ~(form-for-hook-selectors selectors)
        (let [summary# (apply ~'clojure.test/run-tests '~namespaces)]
          (when-not (= "1.5" (System/getProperty "java.specification.version"))
            (shutdown-agents))
          ;; Stupid ant won't let us return anything, so write results to disk
          (with-open [w# (-> (java.io.File. ~result-file)
                             (java.io.FileOutputStream.)
                             (java.io.OutputStreamWriter.))]
            (.write w# (pr-str summary#))))
        (System/exit 0))))

(defn test
  "Run the project's tests. Accepts a list of namespaces for which to run all
tests. If none are given, runs them all." ; TODO: update
  [project & tests]
  (let [tests (map read-string tests)
        nses (if (or (empty? tests) (every? keyword? tests))
               (sort (namespaces-in-dir (:test-path project)))
               tests)
        result (doto (File/createTempFile "lein" "result") .deleteOnExit)
        selectors (map (:test-selectors project) (filter keyword? tests))
        selectors (if (and (empty? selectors)
                           (:default (:test-selectors project)))
                    [(:default (:test-selectors project))]
                    selectors)]
    (when-not (or (every? symbol? nses) (every? keyword? nses))
      (throw (Exception. "Args must be either all namespaces or keywords.")))
    (eval-in-project project (form-for-testing-namespaces
                              nses (.getAbsolutePath result) (vec selectors))
                     #(apply init-args %
                             (if (seq selectors)
                               ["@clojure/test.clj" "@robert/hooke.clj"]
                               ["@clojure/test.clj"])))
    (if (and (.exists result) (pos? (.length result)))
      (let [summary (read-string (slurp (.getAbsolutePath result)))
            success? (zero? (+ (:error summary) (:fail summary)))]
        (if success? 0 1))
      1)))
