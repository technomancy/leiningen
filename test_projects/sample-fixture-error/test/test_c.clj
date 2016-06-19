(ns test-c
  (:require [clojure.test :refer :all]
            [test-a :refer (record-ran)]))

(deftest test-c
  (record-ran :test-c)
  (is (= 1 1)))
