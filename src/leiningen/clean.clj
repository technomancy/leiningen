(ns leiningen.clean
  "Remove compiled class files from project."
  (:use [leiningen.util.file :only [delete-file-recursively]]
        [clojure.java.io :only [file]]))

(defn clean-jar-pred [project]
  (let [default-regex (re-pattern (format "^%s-.*\\.jar$" (:name project)))]
    (fn [f]
      (re-find (:regex-to-clean project default-regex) (.getName f)))))

(defn files-to-clean [project]
  (concat [(:compile-path project)]
          (filter (clean-jar-pred project) (.listFiles (file (:root project))))
          (for [f (:extra-files-to-clean project)]
            (format f (:version project)))))

(defn clean
  "Remove compiled class files and jars from project.

Set :extra-files-to-clean in project.clj to delete other files. Dependency
jars are not deleted; run deps task to delete all jars and get fresh ones."
  [project]
  (println "Cleaning up.")
  (doseq [f (files-to-clean project)]
    (delete-file-recursively f :silently)))
