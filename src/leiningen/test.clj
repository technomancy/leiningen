(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:use [clojure.java.io :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
        [leiningen.compile :only [eval-in-project]]))

(defn- with-version-guard
  "Compatibility test to prevent use of clojure.test with Clojure 1.0."
  [form]
  `(if (and (= 1 (:major *clojure-version*))
            (= 0 (:minor *clojure-version*)))
     (println "\"lein test\" is not compatible with Clojure 1.0 projects.
Please consider upgrading to a newer version of Clojure or using"
              "the lein-test-is plugin.")
     ~form))

(defn form-for-testing-namespaces
  "Return a form that when eval'd in the context of the project will test
each namespace and print an overall summary."
  ([namespaces result-file]
     (form-for-testing-namespaces namespaces 'clojure.test result-file))
  ([namespaces test-package result-file]
     `(do
        (require '~test-package)
        (doseq [n# '~namespaces]
          (require n#))
        (let [resolver# (fn [fname#]
                          (ns-resolve
                           (find-ns '~test-package) fname#))
              summary# (apply (resolver# ~''run-tests) '~namespaces)]
          (when-not (= "1.5" (System/getProperty "java.specification.version"))
            (shutdown-agents))
          (with-open [w# (-> (java.io.File. ~result-file)
                             (java.io.FileOutputStream.)
                             (java.io.OutputStreamWriter.))]
            (.write w# (pr-str summary#))))
        (System/exit 0))))

(defn test
  "Run the project's tests. Accepts a list of namespaces for which to run all
tests. If none are given, runs them all."
  [project & namespaces]
  (let [namespaces (if (empty? namespaces)
                     (sort (find-namespaces-in-dir (file (:test-path project))))
                     (map symbol namespaces))
        result (java.io.File/createTempFile "lein" "result")]
    (eval-in-project project
                     (with-version-guard
                       (form-for-testing-namespaces namespaces
                                                    (.getAbsolutePath result))))
    (if (and (.exists result) (pos? (.length result)))
      (let [summary (read-string (slurp (.getAbsolutePath result)))
            success? (zero? (+ (:error summary) (:fail summary)))]
        (.delete result)
        (if success? 0 1))
      1)))
