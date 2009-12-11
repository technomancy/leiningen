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

(defn test
  "Run the project's tests. Accept a list of namespaces for which to run all
tests for. If none are given, runs them all."
  [project & namespaces]
  (let [namespaces (if (empty? namespaces)
                     (find-namespaces-in-dir (file (:root project) "test"))
                     (map symbol namespaces))]
    ;; It's long and a bit hairy because it has to be self-contained.
    (eval-in-project
     project
     `(do (use ~''clojure.test)
          (let [add-numbers# (fn [a# b#] (if (number? a#)
                                           (+ a# b#) a#))
                summary# (reduce (fn [summary# n#]
                                   (require n#)
                                   (merge-with add-numbers#
                                               summary# (run-tests n#)))
                                 {} '~namespaces)]
            (with-test-out
              (println "\n\n--------------------\nTotal:")
              (report summary#))
            (System/exit (if (successful? summary#) 0 1)))))))
