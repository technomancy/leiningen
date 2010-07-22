(ns leiningen.clean
  "Remove compiled files and dependencies from project."
  (:use [leiningen.jar :only [get-jar-filename get-default-uberjar-name]]
        [clojure.contrib.io :only [file delete-file delete-file-recursively]]))

(defn clean
  "Remove compiled files and dependencies from project."
  [project]
  (println "Cleaning up.")
  (doseq [f [(get-jar-filename project)
             (get-jar-filename project (get-default-uberjar-name project))
             (:compile-path project)]]
    (delete-file-recursively f true)))
