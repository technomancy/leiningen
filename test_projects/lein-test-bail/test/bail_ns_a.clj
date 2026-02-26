(ns bail-ns-a
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]))

(defn record-ran [t]
  (let [file-name (format "%s/lein-test-ran"
                          (System/getProperty "java.io.tmpdir"))]
    (with-open [w (io/writer file-name :append true)]
      (.write w (str t "\n")))))

(deftest failing-test-a
  (record-ran :ns-a)
  (is false "Expected failure"))
