(ns leiningen.with-profile
  "Apply the given task with the profile(s) specified."
  (:require [clojure.string :as string]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [robert.hooke :as hooke]))

(defn ^:internal with-profiles*
  "Apply the given task with a comma-separated profile list."
  [project profiles task-name args]
  (hooke/with-scope
    (let [project (and project (project/set-profiles
                                 (project/project-with-profiles project)
                                 profiles))
          task-name (main/lookup-alias task-name project)]
      (main/apply-task task-name project args))))

(defn profiles-in-group
  [project profile-group]
  (let [profiles (.split profile-group ",")
        prefixes (map first profiles)]
    (cond
     (every? #{\+ \-} prefixes)
     (distinct (reduce
                (fn [result profile]
                  (if (= \+ (first profile))
                    (concat result [(keyword (subs profile 1))])
                    (remove #(= (keyword (subs profile 1)) %) result)))
                (:active-profiles (meta project))
                profiles))

     (not-any? #{\+ \-} prefixes)
     (map keyword profiles)

     :else
     (throw
      (ex-info
       "Profiles in with-profile must either all be qualified, or none qualified"
       {:exit-code 1})))))

(defn ^:no-project-needed ^:higher-order with-profile
  "Apply the given task with the profile(s) specified.

Comma-separated profiles may be given to merge profiles and perform the task.
Colon-separated profiles may be given for sequential profile task application.

A profile list may either be a list of profiles to use, or may specify the
profiles to add or remove from the active profile list using + or - prefixes.

For example:

     lein with-profile user,dev test
     lein with-profile -dev test
     lein with-profile +1.4:+1.4,-dev:base,user test

To list all profiles or show a single one, see the show-profiles task.
For a detailed description of profiles, see `lein help profiles`."
  [project profiles task-name & args]
  (let [profile-groups (seq (.split profiles ":"))
        failures (atom 0)]
    (doseq [profiles (map (partial profiles-in-group project) profile-groups)]
      (when (> (count profile-groups) 1)
        (main/info (format "Performing task '%s' with profile(s): '%s'"
                           task-name (string/join "," (map name profiles)))))
      (binding [main/*exit-process?* false]
        (try
          (with-profiles* project profiles task-name args)
          (catch Exception e
            (main/info
             (format
              "Error encountered performing task '%s' with profile(s): '%s'"
              task-name (string/join "," (map name profiles))))
            (if (and (:exit-code (ex-data e)) (not main/*debug*))
              (main/info (.getMessage e))
              (.printStackTrace e))
            (swap! failures inc)))))
    (when (pos? @failures)
      (main/abort))))
