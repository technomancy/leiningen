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
    (.setWritable f true)
    (io/delete-file f silently)))

(defn clean
  "Remove all files from project's target-path.
  When compile-path isn't under target-path, files under it are not removed
  unless clean-compile-path? is set to true.
  When native-path isn't under target-path, files under it are not removed
  unless clean-native-path? is set to true."
  [project]
  (let [{:keys [target-path
                compile-path clean-compile-path?
                native-path clean-native-path?]} project]
    (if clean-compile-path?
        (delete-file-recursively compile-path :silently))
    (if clean-native-path?
        (delete-file-recursively native-path :silently))
    (delete-file-recursively target-path :silently)))
