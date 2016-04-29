(ns test-a
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [writer]]))

(defn record-ran [t]
  (let [file-name (format "%s/lein-test-ran"
                          (System/getProperty "java.io.tmpdir"))]
    (with-open [w (writer file-name :append true)]
      (.write w (str t "\n")))))

(deftest test-a
  (record-ran :test-a)
  (is (= 1 1)))
