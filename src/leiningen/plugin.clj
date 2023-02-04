(ns leiningen.plugin
  "DEPRECATED. Please use the :user profile instead."
  (:require [leiningen.core.main :as main]))

(defn ^:no-project-needed plugin
  "DEPRECATED. Please use the :user profile instead."
  [& args]
  (main/abort "The plugin task from Leiningen 1.x has been removed."))
