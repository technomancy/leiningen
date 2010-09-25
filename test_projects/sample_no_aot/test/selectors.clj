(ns selectors
  (:use [clojure.test]
        [clojure.java.io]))

(defn record-ran [t]
  (with-open [w (writer "/tmp/lein-test-ran" :append true)]
    (.write w (str t "\n"))))

(deftest ^{:integration true} integration-test
  (record-ran :integration)
  (is true))

(deftest regular
  (record-ran :regular)
  (is true))

(deftest ^{:custom false} not-custom
  (record-ran :not-custom)
  (is true))

(deftest ^{:int2 true} integration-2
  (record-ran :int2)
  (is true))
