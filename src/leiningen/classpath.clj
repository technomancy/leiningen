(ns leiningen.classpath
  (:use (clojure.contrib [io :only (file)]
                         [string :only (join)])
        [leiningen.checkout-deps :only [checkout-deps-paths]])
  (:import org.apache.tools.ant.types.Path))

(defn find-lib-jars
  "Returns a seq of Files for all the jars in the project's library directory."
  [project]
  (filter #(.endsWith (.getName %) ".jar")
          (file-seq (file (:library-path project)))))

(defn make-path
  "Constructs an ant Path object from Files and strings."
  [& paths]
  (let [ant-path (Path. nil)]
    (doseq [path paths]
      (.addExisting ant-path (Path. nil (str path))))
    ant-path))

(defn get-classpath
  "Answer a list of classpath entries for PROJECT."
  [project]
  (concat [(:source-path project)
           (:test-path project)
           (:compile-path project)
           (:resources-path project)]
          (find-lib-jars project)
          (checkout-deps-paths project)))

(defn classpath
  "Print out the classpath in which the project operates, within the
  current directory, in a format suitable for the -classpath option."
  [project]
  (println (join java.io.File/pathSeparatorChar (get-classpath project))))
