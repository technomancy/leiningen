(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]))

(defn deps
  "Download all dependencies.

You should never need to invoke this manually."
  [project]
  (classpath/resolve-dependencies project))
