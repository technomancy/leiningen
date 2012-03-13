(ns leiningen.plugin)

(defn ^:no-project-needed plugin
  "DEPRECATED. Please use the :user profile instead."
  [& args]
  (println "The plugin task has been removed.\n")
  (println "Please see the upgrade guide for instructions on how to use the")
  (println "user profile to specify plugins instead:")
  (println " https://github.com/technomancy/leiningen/wiki/Upgrading")
  1)