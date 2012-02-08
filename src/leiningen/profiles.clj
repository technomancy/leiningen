(ns leiningen.profiles
  (:require [clojure.string]
            [leiningen.core.project :as project]
            [leiningen.core.user :as user]))

(defn ^:no-project-needed profiles
  "List all available profiles."
  [project]
  (->> (merge (deref project/default-profiles)
              (user/profiles)
              (:profiles project))
       keys
       (map name)
       sort
       (clojure.string/join "\n")
       println))
