(ns leiningen.test
  "Run the project's tests."
  (:refer-clojure :exclude [test])
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
        [leiningen.compile :only [eval-in-project]]))

(defn form-for-testing-namespaces
  "Return a form that when eval'd in the context of the project will test
each namespace and print an overall summary."
  [namespaces]
  `(do (use ~''clojure.test)
       (let [add-numbers# (fn [a# b#] (if (number? a#)
                                        (+ a# b#) a#))
             summary# (reduce (fn [summary# n#]
                                (require n# :reload-all)
                                (merge-with add-numbers#
                                            summary#
                                            (clojure.test/run-tests n#)))
                              {} '~namespaces)]
         (clojure.test/with-test-out
           (println "\n\n--------------------\nTotal:")
           (clojure.test/report summary#))
         (when-not (= "1.5" (System/getProperty "java.specification.version"))
           (shutdown-agents)))))

(defn test
  "Run the project's tests. Accept a list of namespaces for which to run all
tests for. If none are given, runs them all."
  [project & namespaces]
  (let [namespaces (if (empty? namespaces)
                     (find-namespaces-in-dir (file (:test-path project)))
                     (map symbol namespaces))]
    (eval-in-project project (form-for-testing-namespaces namespaces))))
