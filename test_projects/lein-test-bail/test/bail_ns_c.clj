(ns bail-ns-c
  (:require [clojure.test :refer [deftest is]]
            [bail-ns-a :refer [record-ran]]))

(deftest passing-test-c
  (record-ran :ns-c)
  (is true))
