(ns leiningen.clean
  "Remove compiled files and dependencies from project."
  (:use [clojure.contrib.java-utils :only [file delete-file
                                           delete-file-recursively]]))

(defn clean [project & args]
  (delete-file (str (:root project) "/" (:name project) ".jar") true)
  (doseq [d [(:compile-path project)
             (str (:root project) "/lib")]]
    (println "Cleaning " d)
    (delete-file-recursively (file d) true)))
