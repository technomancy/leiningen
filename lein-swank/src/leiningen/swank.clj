(ns leiningen.swank
  (:require [swank.swank]))

(defn swank
  "Launch swank server for Emacs to connect."
  ([project port]
     (swank.swank/start-repl (Integer. port)))
  ([project] (swank.swank/start-repl)))
