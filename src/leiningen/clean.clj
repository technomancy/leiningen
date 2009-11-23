(ns leiningen.clean
  "Remove compiled files and dependencies from project."
  (:use [clojure.contrib.java-utils :only [file delete-file
                                           delete-file-recursively]]))

(defn clean [project & args]
  (delete-file (str (:root project) "/" (:name project) ".jar") true)
  (doseq [d ["classes" "lib"]]
    (println "Cleaning " d)
    (doseq [f (.listFiles (file (:root project) d))]
      (delete-file-recursively f true))))
