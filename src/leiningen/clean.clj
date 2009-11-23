(ns leiningen.clean
  "Remove compiled files and dependencies from project."
  (:use [clojure.contrib.java-utils :only [file delete-file
                                           delete-file-recursively]]))

(defn empty-directory
  "Recursively delete all the files in f, but not f itself.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (file f)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))))

(defn clean [project & args]
  (println "Cleaning up")
  (delete-file (str (:root project) "/" (:name project) ".jar") true)
  (delete-file (str (:root project) "/" (:name project) "-standalone.jar") true)
  (delete-file (str (:root project) "/pom-generated.xml") true)
  (doseq [d [(:compile-path project)
             (str (:root project) "/lib")]]
    (empty-directory (file d) true)))
