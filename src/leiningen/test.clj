(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:use [clojure.test]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]))

(defonce old-test-var test-var)

(defn test-var-matching [pred var]
  (when (pred (meta var))
    (old-test-var var)))

(defn merge-predicates [preds]
  (fn [t] (every? #(% t) preds)))

(defn run-matching [project preds]
  (binding [test-var (partial test-var-matching (merge-predicates preds))]
    (doseq [n (find-namespaces-in-dir
               (file (:root project) "test"))]
      (require n)
      (run-tests n))))

(defn test
  "Run the projects tests. Accept a list of predicates called with each test
var's metadata. Does not support anonymous fns; works best with keywords."
  [project & args]
  (let [preds (if (empty? args)
                [identity]
                (map (comp eval read-string) args))]
    ;; TODO: System/exit appropriately (depends on Clojure ticket #193)
    (run-matching project preds)))
