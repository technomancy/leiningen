(ns leiningen.classpath
  "Print the classpath of the current project."
  (:require [leiningen.core.classpath :as classpath]
            [clojure.string :as str]))

(defn get-classpath-string [project]
  (str/join java.io.File/pathSeparatorChar (classpath/get-classpath project)))

(defn classpath
  "Write the classpath of the current project to output-file.

With no arguments, print the classpath to stdout.

Suitable for java's -cp option."
  ([project]
     (println (get-classpath-string project)))
  ([project output-file]
     (spit output-file (get-classpath-string project))))
