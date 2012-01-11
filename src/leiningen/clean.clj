(ns leiningen.clean
  "Remove compiled class files from project."
  (:use [clojure.java.io :only [file delete-file]]))

(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (System/gc) ; This sometimes helps release files for deletion on windows.
  (let [f (file f)]
    (if (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (delete-file f silently)))

(defn clean
  "Remove all files from project's target-path."
  [project]
  (delete-file-recursively (:target-path project) :silently))
