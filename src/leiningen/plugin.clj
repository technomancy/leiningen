(ns leiningen.plugin
  "DEPRECATED. Please use the :user profile instead."
  (:require [leiningen.core.main :as main]))

(defn ^:no-project-needed plugin
  "DEPRECATED. Please use the :user profile instead."
  [& args]
  (main/abort "The plugin task has been removed.\n"
              "\nPlease see the upgrade guide for instructions on how to use"
              "the user profile to\nspecify plugins instead:"
              "https://codeberg.org/leiningen/leiningen/wiki/Upgrading"))
