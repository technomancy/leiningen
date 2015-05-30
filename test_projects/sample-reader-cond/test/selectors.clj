(ns selectors
  (:use [clojure.test]
        [clojure.java.io]))

(defn record-ran [t]
  (let [file-name (format "%s/lein-test-ran"
                          (System/getProperty "java.io.tmpdir"))]
    (with-open [w (writer file-name :append true)]
      (.write w (str t "\n")))))

