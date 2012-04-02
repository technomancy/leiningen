(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]
            [clojure.pprint :as pp]))

;;; TODO: Get rid of this recursion
(defn- print-tree
  ([tree level increment]
     (doseq [[n c] tree]
       (println (str (apply str (repeat level \space))) n)
       (when c
         (print-tree c (+ level increment) increment))))
  ([tree increment]
     (print-tree tree 0 increment)))

(defn deps
  "Download all dependencies.

You should never need to invoke this manually."
  ([project]
     (deps project nil))
  ([project tree]
     (if (= tree ":tree")
       (print-tree (classpath/dependency-hierarchy :dependencies project) 4)
       (classpath/resolve-dependencies :dependencies project))))
