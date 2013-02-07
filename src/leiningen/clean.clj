(ns leiningen.clean
  "Remove all files from project's target-path."
  (:require [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.utils :as utils]))

(defn real-directory?
  "Returns true if this file is a real directory, false if it is a symlink or a
  normal file."
  [f]
  (if (= :windows (eval/get-os))
    (.isDirectory f)
    (and (.isDirectory f)
         (not (utils/symlink? f)))))

(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (io/file f)]
    (when (real-directory? f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (io/delete-file f silently)
    ;; This sometimes helps release files for deletion on windows, but
    ;; is slow as the dickens.
    (when (= :windows (eval/get-os))
      (System/gc))))

(defn clean
  "Remove all files from project's target-path."
  [project]
  (delete-file-recursively (:target-path project) :silently))
