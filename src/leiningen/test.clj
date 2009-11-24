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

(defn run-matching [project preds]
  (binding [test-var (partial test-var-matching (merge-predicates preds))]
    (doseq [n (find-namespaces-in-dir
               (file (:root project) "test"))]
      (require n)
      (binding [report lein-report]
        (run-tests n)))
    (super-summary)))

(defn test
  "Run the projects tests. Accept a list of predicates called with each test
var's metadata. Does not support anonymous fns; works best with keywords."
  [project & args]
  ;; TODO: eval args; if they're namespaces, run their tests; if they're
  ;; ifn? then use them as predicates
  (let [preds (if (empty? args)
                [identity]
                (map (comp eval read-string) args))]
    ;; TODO: System/exit appropriately (depends on Clojure ticket #193)
    (run-matching project preds)))
