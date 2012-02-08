(ns leiningen.javac
  "Compile Java source files."
  (:use [leiningen.classpath :only [get-classpath-string]]
        [clojure.string :only [join]]
        [clojure.java.io :only [file]])
  (:import javax.tools.ToolProvider))

(defn extract-java-source
  "Find all of the Java source files in a directory."
  [dir]
  (filter #(.endsWith % ".java")
          (map #(.getPath %) (file-seq (file dir)))))

(defn javac-options [project]
  (into-array 
    String
    (concat (:javac-options project) 
            ["-cp" (get-classpath-string project)
             "-d" (:compile-path project)]
            (mapcat extract-java-source (:java-source-path project)))))

(defn- run-javac-task
  "Compile the given task spec."
  [project]
  (-> project :compile-path file .mkdirs)
  (.run (ToolProvider/getSystemJavaCompiler) nil nil nil (javac-options project)))

(defn javac
  "Compile Java source files.

Add a :java-source-path key to project.clj to specify where to find them."
  [project]
  (run-javac-task project))

