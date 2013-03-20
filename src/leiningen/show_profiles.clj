(ns leiningen.show-profiles
  (:require [clojure.string]
            [clojure.pprint :as pprint]
            [leiningen.core.project :as project]))

(defn ^:no-project-needed show-profiles
  "List all available profiles or display one if given an argument."
  ([project]
     (->> (project/read-profiles project)
          (keys)
          (map name)
          (sort)
          (clojure.string/join "\n")
          (println)))
  ([project profile]
     (-> (all-profiles project)
         (get (keyword profile))
         (pprint/pprint))
     (flush)))
