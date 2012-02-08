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

(defn javac-options [project files args]
  (into-array 
    String
    (concat (:javac-options project)
            args
            ["-cp" (get-classpath-string project)
             "-d" (:compile-path project)]
            files)))

(defn- run-javac-task
  "Compile the given task spec."
  [project args]
  (let [files (mapcat extract-java-source (:java-source-path project))
        compile-path (:compile-path project)]
    (when (pos? (count files))
      (println "Compiling" (count files) "source files to" compile-path) 
      (.mkdirs (file compile-path)) 
      (.run (ToolProvider/getSystemJavaCompiler) 
            nil nil nil
            (javac-options project files args)))))

(defn javac
  "Compile Java source files.

Add a :java-source-path key to project.clj to specify where to find them.
Any options passed will be given to javac. One place where this can be useful
is `lein javac -verbose`."
  [project & args]
  (run-javac-task project args))

