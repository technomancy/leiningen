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
  `(do
     (require ~''clojure.test)
     (if (and (= 1 (:major *clojure-version*))
              (= 0 (:minor *clojure-version*)))
       (println "\"lein test\" is not compatible with Clojure 1.0.\n
Please consider upgrading to a newer version of Clojure or using"
                "the lein-test-is plugin.")
       (let [resolver# (fn [fname#]
                         (ns-resolve
                          (find-ns 'clojure.test) fname#))
             add-numbers# (fn [a# b#] (if (number? a#)
                                        (+ a# b#) a#))
             summary# (reduce (fn [summary# n#]
                                (require n# :reload-all)
                                (merge-with add-numbers#
                                            summary#
                                            ((resolver# ~''run-tests) n#)))
                              {} '~namespaces)]
         ((resolver# ~''with-test-out)
           (println "\n\n--------------------\nTotal:")
           ((resolver# ~''report) summary#))
         (when-not (= "1.5" (System/getProperty "java.specification.version"))
           (shutdown-agents))))))

(defn test
  "Run the project's tests. Accept a list of namespaces for which to run all
tests for. If none are given, runs them all."
  [project & namespaces]
  (let [namespaces (if (empty? namespaces)
                     (find-namespaces-in-dir (file (:test-path project)))
                     (map symbol namespaces))]
    (eval-in-project project (form-for-testing-namespaces namespaces))))
