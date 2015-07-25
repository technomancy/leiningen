(ns clj-test
  (:use [clojure.test]
        [selectors :only [record-ran]]))

(deftest clojure-test
  (record-ran :clj-test)
  (is true))
