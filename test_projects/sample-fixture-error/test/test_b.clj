(ns test-b
  (:require [clojure.test :refer :all]
            [test-a :refer [record-ran]]))

(use-fixtures :once
  (fn [& _]
    (throw (Exception. "Don't panic. This is an expected exception."))))

(deftest test-b
  (record-ran :test-b)
  (is (= 1 1)))
