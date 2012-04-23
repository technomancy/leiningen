(ns leiningen.with-profile
  (:require [leiningen.core.main :as main]
            [leiningen.core.project :as project]))

(defn- with-profile*
  "Apply the given task with a comma-separated profile list."
  [project profiles task-name & args]
  (let [profiles (map keyword (.split profiles ","))
        project (project/merge-profiles project profiles)]
    (main/apply-task task-name project args)))

(defn with-profile
  "Apply the given task with the profile(s) specified.

Comma-separated profiles may be given to merge profiles and perform the task.
Colon-separated profiles may be given for sequential profile task application."
  [project profiles task-name & args]
  (let [profile-groups (seq (.split profiles ":"))
        project (:without-profiles (meta project) project)
        failures (atom 0)]
    (doseq [profile-group profile-groups]
      (binding [main/*exit-process?* false]
        (main/info (format "Performing task '%s' with profile(s): '%s'"
                           task-name profile-group))
        (try (apply with-profile* project profile-group task-name args)
             (catch Exception e
               (main/info (format "Error encountered performing task '%s' with profile(s): '%s'"
                           task-name profile-group))
               (.printStackTrace e)
               (swap! failures inc)))))
    (when (pos? @failures)
      (main/abort "Failed."))))
