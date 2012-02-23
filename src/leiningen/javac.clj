(ns leiningen.javac
  "Compile Java source files."
  (:use [leiningen.classpath :only [get-classpath-string]]
        [clojure.string :only [join]]
        [clojure.java.io :only [file]])
  (:import javax.tools.ToolProvider))

;; There is probably a more efficient way to do this, but this is cool
;; too.
(defn extract-java-source
  "Find all of the Java source files in a directory."
  [dir]
  (filter #(.endsWith % ".java")
          (map #(.getPath %) (file-seq (file dir)))))

;; Tool's .run method expects the last argument to be an array of
;; strings, so that's what we'll return here.
(defn javac-options 
  "Compile all sources of possible options and add important defaults.
  Result is a String java array of options."
  [project files args]
  (into-array 
    String
    (concat (:javac-options project)
            args
            ["-cp" (get-classpath-string project)
             "-d" (:compile-path project)]
            files)))

;; We can't really control what is printed here. We're just going to
;; allow `.run` to attach in, out, and err to the standard streams. This
;; should have the effect of compile errors being printed. javac doesn't
;; actually output any compilation info unless it has to (for an error)
;; or you make it do so with `-verbose`.
(defn- run-javac-task
  "Run javac to compile all source files in the project."
  [project args]
  (let [files (mapcat extract-java-source (:java-source-paths project))
        compile-path (:compile-path project)]
    (when (pos? (count files))
      (println "Compiling" (count files) "source files to" compile-path) 
      (.mkdirs (file compile-path)) 
      (.run (ToolProvider/getSystemJavaCompiler) 
            nil nil nil
            (javac-options project files args)))))

(defn javac
  "Compile Java source files.

Add a :java-source-paths key to project.clj to specify where to find them.
Any options passed will be given to javac. One place where this can be useful
is `lein javac -verbose`."
  [project & args]
  (run-javac-task project args))

