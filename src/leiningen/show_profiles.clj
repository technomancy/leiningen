(ns leiningen.show-profiles
  (:require [clojure.string]
            [clojure.pprint :as pprint]
            [leiningen.core.project :as project]
            [leiningen.core.user :as user]))

(defn- all-profiles [project]
  (merge @project/default-profiles
         (user/profiles)
         (:profiles project)))

(defn ^:no-project-needed show-profiles
  "List all available profiles or display one if given an argument."
  ([project]
     (->> (all-profiles project)
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
