(ns selectors
  (:use [clojure.test]
        [clojure.java.io]))

(defn record-ran [t]
  (let [file-name (format "%s/lein-test-ran"
                          (System/getProperty "java.io.tmpdir"))]
    (with-open [w (writer file-name :append true)]
      (.write w (str t "\n")))))

(use-fixtures :each (fn [t] (record-ran :fixture) (t)))

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
