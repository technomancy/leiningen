(ns leiningen.release
  "Perform :release-tasks."
  (:require [leiningen.core.main :as main]
            [leiningen.core.project]))

(def ^:dynamic *level* :patch)

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
      (main/abort "Unrecognized version string:" version-string))))

(defn version-map->string
  "Given a version-map, return a string representing the version."
  [version-map]
  (let [{:keys [major minor patch qualifier]} version-map]
    (if qualifier
      (str major "." minor "." patch "-" qualifier)
      (str major "." minor "." patch))))

(defn next-qualifier [sublevel qualifier]
  (let [pattern (re-pattern (str sublevel "([0-9]+)"))
        [_ n] (and qualifier (re-find pattern qualifier))]
    (str sublevel (inc (Integer. (or n 0))))))

(defn bump-version-map
  "Given version as a map of the sort returned by parse-semantic-version, return
  a map of the version incremented in the level argument. Add qualifier unless
  releasing non-snapshot."
  [{:keys [major minor patch qualifier]} level]
  (case (keyword (name level))
    :major {:major (inc major) :minor 0 :patch 0 :qualifier "SNAPSHOT"}
    :minor {:major major :minor (inc minor) :patch 0 :qualifier "SNAPSHOT"}
    :patch {:major major :minor minor :patch (inc patch) :qualifier "SNAPSHOT"}
    :alpha {:major major :minor minor :patch (inc patch)
            :qualifier (next-qualifier "alpha" qualifier)}
    :beta {:major major :minor minor :patch (inc patch)
           :qualifier (next-qualifier "beta" qualifier)}
    :rc {:major major :minor minor :patch (inc patch)
         :qualifier (next-qualifier "RC" qualifier)}
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

The default list of release tasks is as follows:

  :release-tasks [[\"vcs\" \"assert-committed\"]
                  [\"change\" \"version\"
                   \"leiningen.release/bump-version\" \"release\"]
                  [\"vcs\" \"commit\"]
                  [\"vcs\" \"tag\"]
                  [\"deploy\"]
                  [\"change\" \"version\" \"leiningen.release/bump-version\"]
                  [\"vcs\" \"commit\"]
                  [\"vcs\" \"push\"]]

First change the version stored in project.clj, then commit that change, tag
this commit to with the release version indicated, deploy to the Maven release
repository, then change to the next snapshot version in project.clj, commit
that change, and push to the default remote version control repository.

A key point to note is that this default set of :release-tasks requires a clean
working directory as far as the current version control system is concerned.
This ensures that the `vcs commit` tasks will only save changes made to
project.clj made by the `change version` tasks.

This behavior can be overridden by setting :release-tasks a vector in which
every element is either a task name or a collection in which the first element
is a task name and the rest are arguments to that task.

The release task takes a single argument which should be one of :major,
:minor, :patch, :alpha, :beta, or :rc to indicate which version level to
bump. If none is given, it defaults to :patch."
  ([project] (release project (str *level*)))
  ([project level]
     (binding [*level* (read-string level)]
       (doseq [task (:release-tasks project)]
         (let [[task-name & task-args] (if (vector? task) task [task])
               task-name (main/lookup-alias task-name project)
               current-project (leiningen.core.project/read)]
           (main/apply-task task-name current-project task-args))))))
