(ns leiningen.compile
  "Compile the namespaces listed in project.clj or all namespaces in src."
  (:require lancet)
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
        [leiningen.deps :only [deps]])
  (:refer-clojure :exclude [compile])
  (:import org.apache.tools.ant.taskdefs.Java
           (org.apache.tools.ant.types Environment$Variable Path)))

(defn namespaces-to-compile
  "Returns a seq of the namespaces which need compiling."
  [project]
  (for [n (or (:namespaces project)
              (find-namespaces-in-dir (file (:source-path project))))
        :let [ns-file (str (-> (name n)
                               (.replaceAll "\\." "/")
                               (.replaceAll "-" "_")))]
        :when (> (.lastModified (file (:source-path project)
                                      (str ns-file ".clj")))
                 (.lastModified (file (:compile-path project)
                                      (str ns-file "__init.class"))))]
    n))

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

(defn eval-in-project
  "Executes form in an isolated classloader with the classpath and compile path
  set correctly for the project. Pass in a handler function to have it called
  with the java task right before executing if you need to customize any of its
  properties (classpath, library-path, etc)."
  [project form & [handler]]
  (let [java (Java.)]
    (.setProject java lancet/ant-project)
    (.addSysproperty java (doto (Environment$Variable.)
                            (.setKey "clojure.compile.path")
                            (.setValue (:compile-path project))))
    (.setClasspath java (apply make-path
                               (:source-path project)
                               (:test-path project)
                               (:compile-path project)
                               (:resources-path project)
                               (find-lib-jars project)))
    (.setClassname java "clojure.main")
    (.setValue (.createArg java) "-e")
    (.setValue (.createArg java) (prn-str form))
    ;; to allow plugins and other tasks to customize
    (when handler (handler java))
    (.execute java)))

(defn compile
  "Ahead-of-time compile the project. Looks for all namespaces under src/
unless a list of :namespaces is provided in project.clj."
  [project]
  (deps project :skip-dev)
  (.mkdir (file (:compile-path project)))
  (let [namespaces (namespaces-to-compile project)]
    (when (seq namespaces)
      (eval-in-project project
                       `(doseq [namespace# '~namespaces]
                          (println "Compiling" namespace#)
                          (clojure.core/compile namespace#))))))
