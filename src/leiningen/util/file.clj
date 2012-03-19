(ns leiningen.util.file
  (:use [clojure.java.io :only [file delete-file]]))

(def tmp-dir (System/getProperty "java.io.tmpdir"))

(defn unique-lein-tmp-dir []
  (file tmp-dir (str "lein-" (java.util.UUID/randomUUID))))

;; grumble, grumble; why didn't this make it into clojure.java.io?
(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (file f)]
    (if (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (delete-file f silently)))
