(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]
            [clojure.pprint :as pp]))

(defn- print-tree
  ([tree increment level]
     (doseq [[dep deps] tree]
       (println (str (apply str (repeat level \space))) (pr-str dep))
       (when deps
         (print-tree deps increment (+ level increment)))))
  ([tree increment]
     (print-tree tree increment 0)))

(defn deps
  "Download all dependencies.

You should never need to invoke this manually."
  ([project]
     (deps project nil))
  ([project style]
     (if (= style ":tree")
       (print-tree (classpath/dependency-hierarchy :dependencies project) 2)
       (classpath/resolve-dependencies :dependencies project))))
