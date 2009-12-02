(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  ;; When using -Xbootclasspath transitive requires break, so we need
  ;; to require stuff that clojure.test depends on explicitly:
  (:require [clojure walk template stacktrace])
  (:use [clojure.test]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
        [leiningen.compile :only [eval-in-project]]))

(def report-fns
     '(let [aggregates (ref [])]

        (defmethod report :summary [m]
          (with-test-out
            (println "\nRan" (:test m) "tests containing"
                     (+ (:pass m) (:fail m) (:error m)) "assertions.")
            (println (:fail m) "failures," (:error m) "errors."))
          (dosync (commute aggregates conj m))
          (= 0 (:fail m) (:error m)))

        (defn test-namespace [all-passed? n]
          (require n)
          (let [this-passed? (run-tests n)]
            (and all-passed? this-passed?)))

        (defn super-summary []
          (with-test-out
            (println "\n\n--------------------\nTotal:"))
          (report (apply merge-with (fn [a b]
                                      (if (number? a)
                                        (+ a b)
                                        a))
                         @aggregates)))))

(defn test
  "Run the project's tests. Accept a list of namespaces for which to run all
tests for. If none are given, runs them all."
  [project & namespaces]
  (let [namespaces (if (empty? namespaces)
                     (find-namespaces-in-dir (file (:root project) "test"))
                     (map symbol namespaces))]
    (eval-in-project
     project
     `(do
        (use ~''clojure.test)
        ~report-fns
        (let [passed?# (reduce ~'test-namespace true '~namespaces)]
          (~'super-summary)
          (System/exit (if passed?# 0 1)))))))
