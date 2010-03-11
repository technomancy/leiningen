(ns test-jar
  (:use [clojure.test]
        [leiningen.core :only [defproject]]
        [leiningen.jar]))

(defproject mock-project "1.0" :main foo.one-two.three-four.bar)

(deftest test-jar
  (is (= "foo.one_two.three_four.bar"
         (.getValue (.getMainAttributes (make-manifest mock-project)) "Main-Class"))))
