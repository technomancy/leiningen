(ns leiningen.clean
  (:use [clojure.contrib.java-utils :only [file delete-file
                                           delete-file-recursively]]))

(defn clean [project & args]
  (delete-file (str (:root project) "/" (:name project) ".jar") true)
  (doseq [d ["classes" "lib"]]
    (println "Cleaning " d)
    (delete-file-recursively (file (:root project) d) true)))
