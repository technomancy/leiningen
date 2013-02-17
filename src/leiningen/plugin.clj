(ns leiningen.plugin
  (:require [leiningen.core.logger :as log]))

(defn ^:no-project-needed plugin
  "DEPRECATED. Please use the :user profile instead."
  [& args]
  (log/abort 
  	"The plugin task has been removed.\n"
    "Please see the upgrade guide for instructions on how"
    "to use the user profile to specify plugins instead:\n"
    "https://github.com/technomancy/leiningen/wiki/Upgrading"))