(ns leiningen.with-profile
  (:require [leiningen.main :as main]
            [leiningen.core.project :as project]))

(defn with-profile
  "Apply the given task with the profile(s) specified.

Multiple comma-separated profiles may be given."
  [project profiles task-name & args]
  (let [profiles (map keyword (.split profiles ","))
        project (project/merge-profiles project profiles)]
    (main/apply-task task-name project args)))