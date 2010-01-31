(ns test-nom-nom-nom
  (:use [clojure.test]))

(deftest should-use-1.1.0-SNAPSHOT
  (is (= "1.1.0-master-SNAPSHOT" (clojure-version))))
