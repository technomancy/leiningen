(ns leiningen.clean
  "Remove compiled files and dependencies from project."
  (:use [clojure.contrib.io :only [file delete-file delete-file-recursively]]))

(defn empty-directory
  "Recursively delete all the files in f, but not f itself.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (file f)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))))

(defn clean [project]
  (println "Cleaning up")
  (delete-file (str (:jar-dir project) "/" (:name project) ".jar") true)
  (delete-file (str (:jar-dir project) "/" (:name project) "-standalone.jar")
               true)
  (doseq [d [(:compile-path project)
             (:library-path project)
             ;; This must be hard-coded since it's needed in bin/lein
             "lib/dev"]]
    (empty-directory (file d) true)))
