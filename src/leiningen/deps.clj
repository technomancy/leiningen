(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]
            [clojure.pprint :as pp]))

(defn deps
  "Download all dependencies.

You should never need to invoke this manually."
  ([project]
     (deps project nil))
  ([project tree]
     (if (= tree ":tree")
       (do (pp/pprint (classpath/dependency-hierarchy :dependencies project))
           (flush))
       (classpath/resolve-dependencies :dependencies project))))
