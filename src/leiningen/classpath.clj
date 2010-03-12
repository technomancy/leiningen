(ns leiningen.classpath
  (:use (clojure.contrib [seq-utils :only (flatten)]
                         [str-utils :only (str-join)])
        (leiningen [compile :only (find-lib-jars make-path)])))

(defn get-classpath
  "Answer a list of classpath entries for PROJECT."
  [project]
  (flatten [(:source-path project)
            (:test-path project)
            (:compile-path project)
            (:resources-path project)
            (find-lib-jars project)]))

(defn classpath
  "Print out the classpath in which the project operates, within the
  current directory, in a format suitable for the -classpath option."
  [project]
  (println (str-join java.io.File/pathSeparatorChar (get-classpath project))))
