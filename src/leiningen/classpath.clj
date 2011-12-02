(ns leiningen.classpath
  "Print the classpath of the current project."
  (:require [leiningen.core.classpath :as classpath]
            [clojure.string :as str]))

(defn get-classpath-string [project]
  (str/join java.io.File/pathSeparatorChar (classpath/get-classpath project)))

(defn classpath
  "Print the classpath of the current project.

Suitable for java's -classpath option.

Warning: due to a bug in ant, calling this task with :local-repo-classpath set
when the dependencies have not been fetched will result in spurious output before
the classpath. In such cases, pipe to tail -n 1."
  [project]
  (println (get-classpath-string project)))
