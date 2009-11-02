(ns leiningen.test
  (:refer-clojure :exclude [test])
  (:use [clojure.test]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]))

(defonce old-test-var test-var)

(defn test-var-matching [pred var]
  (when (pred (meta var))
    (old-test-var var)))

(defn run-matching [pred namespaces]
  (binding [test-var (partial test-var-matching pred)]
    (apply run-tests namespaces)))

(defn test
  "Run the projects tests. Second argument is an optional filter predicate that
  is called with each test's metadata map."
  [project & [pred]]
  (let [namespaces (find-namespaces-in-dir (file (:root project) "test"))
        runner (if pred
                 (partial run-matching (eval pred))
                 run-tests)]
    (doseq [n namespaces]
      (require n))
    (apply runner namespaces)))
