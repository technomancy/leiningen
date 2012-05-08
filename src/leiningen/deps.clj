(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [clojure.pprint :as pp])
  (:import (org.sonatype.aether.resolution DependencyResolutionException)))

(defn- print-tree
  ([tree increment level]
     (doseq [[dep deps] tree]
       (println (str (apply str (repeat level \space))) (pr-str dep))
       (when deps
         (print-tree deps increment (+ level increment)))))
  ([tree increment]
     (print-tree tree increment 0)))

(defn deps
  "Download or show all dependencies.

To show the full dependency tree for the current project, run:

    lein deps :tree

To manually have Leiningen download all missing or out-of-date
dependencies, you could run `lein deps`, but that's generally not
necessary, since Leiningen automatically checks for and downloads
those."
  ([project]
     (deps project nil))
  ([project style]
     (try
       (if (= style ":tree")
         (print-tree (classpath/dependency-hierarchy :dependencies project) 2)
         (classpath/resolve-dependencies :dependencies project))
       (catch DependencyResolutionException e
         (main/abort (.getMessage e))))))
