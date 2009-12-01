(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:use [clojure.test]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]))

(let [orig-report report
      aggregates (ref [])]
  (defn lein-report [event]
    (when (= (:type event) :summary)
      (dosync (commute aggregates conj event)))
    (orig-report event))

  (defn super-summary []
    (with-test-out
      (println "\n\n--------------------\nTotal:"))
    (orig-report (apply merge-with (fn [a b]
                                     (if (number? a)
                                       (+ a b)
                                       a))
                        @aggregates))))

(defn test
  "Run the project's tests. Accept a list of namespaces for which to run all
tests for. If none are given, runs them all."
  [project & namespaces]
  ;; TODO: System/exit appropriately (depends on Clojure ticket #193)
  (doseq [n (if (empty? namespaces)
              (find-namespaces-in-dir (file (:root project) "test"))
              (map symbol namespaces))]
      (require n)
      (binding [report lein-report]
        (run-tests n))))
