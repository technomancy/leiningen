(ns leiningen.static-classpath
  "Print the classpath of the current project without loading code."
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.project :as project]
            [clojure.java.io :as io])
  (:import (java.io PushbackReader)))

(defn- safely-read-project [project]
  (with-open [rdr (PushbackReader. (io/reader (io/file (:root project)
                                                       "project.clj")))]
    (binding [*read-eval* false]
      (let [items (repeatedly #(read {:eof ::eof} rdr))
            items (take-while #(not= ::eof %) items)
            [_defproject project-name version & rest] (last items)]
        (merge {:name (name project-name)
                :group (or (namespace project-name)
                           (name project-name))
                :version version
                :root (:root project)}
               project/defaults
               (apply hash-map rest))))))

(defn ^:no-project-needed static-classpath
  "Write the classpath of the current project to output-file or stdout.

Does not load any plugins or other project code; should be safe to run on
untrusted projects; however, may be less accurate than `classpath`.
Suitable for use in static analysis."
  ([project]
   (classpath/classpath (safely-read-project project)))
  ([project output-file]
   (classpath/classpath (safely-read-project project) output-file)))
