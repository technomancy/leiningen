(ns leiningen.clean
  "Remove compiled class files from project."
  (:use [leiningen.jar :only [get-jar-filename get-default-uberjar-name]]
        [leiningen.util.file :only [delete-file-recursively]]))

(defn clean
  "Remove compiled class files from project. Jars are not deleted; run deps task
to delete all jars and get fresh ones."
  [project]
  (println "Cleaning up.")
  (doseq [f [(get-jar-filename project)
             (get-jar-filename project (get-default-uberjar-name project))
             (:compile-path project)]]
    (delete-file-recursively f true)))
