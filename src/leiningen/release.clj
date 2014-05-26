(ns leiningen.release
  "Perform :release-tasks."
  (:require [leiningen.core.main :as main]
            [leiningen.core.project]))

(def ^:dynamic *level* "patch")

(defn parse-semantic-version [version-string]
  "Create map representing the given version string. Raise exception if the
  string does not follow guidelines setforth by Semantic Versioning 2.0.0,
  http://semver.org/"
  ;; <MajorVersion>.<MinorVersion>.<PatchVersion>[-<BuildNumber | Qualifier >]
  (let [version-map (->> (re-matches #"(\d+)\.(\d+)\.(\d+).*" version-string)
                         (drop 1)
                         (map #(Integer/parseInt %))
                         (zipmap [:major :minor :patch]))
        qualifier (last (re-matches #".*-(.+)?" version-string))]
    (if-not (empty? version-map)
      (merge version-map {:qualifier qualifier})
      (throw (Exception. "Unrecognized version string.")))))

(defn version-map->string
  "Given a version-map, return a string representing the version."
  [version-map]
  (let [{:keys [major minor patch qualifier]} version-map]
    (if qualifier
      (str major "." minor "." patch "-" qualifier)
      (str major "." minor "." patch))))

(defn bump-version-map
  "Given version as a map of the sort returned by parse-semantic-version, return
  a map of the version incremented in the level argument. Add qualifier unless
  releasing non-snapshot."
  [{:keys [major minor patch qualifier]} level]
  (case (keyword level)
    :major {:major (inc major) :minor 0 :patch 0 :qualifier "SNAPSHOT"}
    :minor {:major major :minor (inc minor) :patch 0 :qualifier "SNAPSHOT"}
    :patch {:major major :minor minor :patch (inc patch) :qualifier "SNAPSHOT"}
    :release {:major major :minor minor :patch patch}))

(defn bump-version
  "Given a version string, return the bumped version string -
   incremented at the indicated level. Add qualifier unless releasing
   non-snapshot. Level defaults to *level*."
  [version-str & [level]]
  (-> version-str
      (parse-semantic-version)
      (bump-version-map (or level *level*))
      (version-map->string)))



(defn ^{:subtasks []} release
  "Perform release tasks.

TODO: document default :release-tasks and how to change them."
  [project level]
  ;; TODO: how to propagate level arg to inc-version function? binding?
  (binding [*level* level]
    (doseq [task (:release-tasks project)]
      (let [[task-name & task-args] (if (vector? task) task [task])
            task-name (main/lookup-alias task-name project)
            current-project (leiningen.core.project/read)]
        (main/apply-task task-name current-project task-args)))))
