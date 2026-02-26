(ns bail-ns-b
  (:require [clojure.test :refer [deftest is]]
            [bail-ns-a :refer [record-ran]]))

(deftest failing-test-b
  (record-ran :ns-b)
  (is false "Expected failure"))
