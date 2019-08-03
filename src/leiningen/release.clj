(ns leiningen.release
  "Perform :release-tasks."
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]))

(def ^:dynamic *level* nil)

(defn string->semantic-version [version-string]
  "Create map representing the given version string. Returns nil if the
  string does not follow guidelines setforth by Semantic Versioning 2.0.0,
  http://semver.org/"
  ;; <MajorVersion>.<MinorVersion>.<PatchVersion>[-<Qualifier>][-SNAPSHOT]
  (if-let [[_ major minor patch qualifier snapshot]
           (re-matches
            #"(\d+)\.(\d+)\.(\d+)(?:-(?!SNAPSHOT)([^\-]+))?(?:-(SNAPSHOT))?"
            version-string)]
    (->> [major minor patch]
         (map #(Integer/parseInt %))
         (zipmap [:major :minor :patch])
         (merge {:qualifier qualifier
                 :snapshot snapshot}))))

(defn parse-semantic-version [version-string]
  "Create map representing the given version string. Aborts with exit code 1
  if the string does not follow guidelines setforth by Semantic Versioning 2.0.0,
  http://semver.org/"
  (or (string->semantic-version version-string)
      (main/abort "Unrecognized version string:" version-string)))

(defn version-map->string
  "Given a version-map, return a string representing the version."
  [version-map]
  (let [{:keys [major minor patch qualifier snapshot]} version-map]
    (cond-> (str major "." minor "." patch)
            qualifier (str "-" qualifier)
            snapshot (str "-" snapshot))))

(defn next-qualifier
  "Increments and returns the qualifier.  If an explicit `sublevel`
  is provided, then, if the original qualifier was using that sublevel,
  increments it, else returns that sublevel with \"1\" appended.
  Supports empty strings for sublevel, in which case the return value
  is effectively a BuildNumber."
  ([qualifier]
   (if-let [[_ sublevel] (re-matches #"([^\d]+)?(?:\d+)?"
                                         (or qualifier ""))]
     (next-qualifier sublevel qualifier)
     "1"))
  ([sublevel qualifier]
   (let [pattern (re-pattern (str sublevel "([0-9]+)"))
         [_ n] (and qualifier (re-find pattern qualifier))]
     (str sublevel (inc (Integer. (or n 0)))))))

(defn bump-version-map
  "Given version as a map of the sort returned by parse-semantic-version, return
  a map of the version incremented in the level argument.  Always returns a
  SNAPSHOT version, unless the level is :release.  For :release, removes SNAPSHOT
  if the input is a SNAPSHOT, removes qualifier if the input is not a SNAPSHOT."
  [{:keys [major minor patch qualifier snapshot]} level]
  (let [level (or level
                  (if qualifier :qualifier)
                  :patch)]
    (case (keyword (name level))
      :major {:major (inc major) :minor 0 :patch 0 :qualifier nil :snapshot "SNAPSHOT"}
      :minor {:major major :minor (inc minor) :patch 0 :qualifier nil :snapshot "SNAPSHOT"}
      :patch {:major major :minor minor :patch (inc patch) :qualifier nil :snapshot "SNAPSHOT"}
      :alpha {:major major :minor minor :patch patch
              :qualifier (next-qualifier "alpha" qualifier)
              :snapshot "SNAPSHOT"}
      :beta {:major major :minor minor :patch patch
             :qualifier (next-qualifier "beta" qualifier)
             :snapshot "SNAPSHOT"}
      :rc {:major major :minor minor :patch patch
           :qualifier (next-qualifier "RC" qualifier)
           :snapshot "SNAPSHOT"}
      :qualifier {:major major :minor minor :patch patch
                  :qualifier (next-qualifier qualifier)
                  :snapshot "SNAPSHOT"}
      :release (merge {:major major :minor minor :patch patch}
                      (if snapshot
                        {:qualifier qualifier :snapshot nil}
                        {:qualifier nil :snapshot nil})))))

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
  ([project] (release project *level*))
  ([project level]
     (binding [*level* (if level (read-string level))]
       (let [release-tasks (:release-tasks project)
             task-count (count release-tasks)]
         (doseq [[i task] (map vector (range 1 (inc task-count)) release-tasks)]
           (apply main/info "[" i "/" task-count "] Running lein" task)
           (let [current-project (project/init-project (project/read))]
             (main/resolve-and-apply current-project task)))))))

;; support existing release plugin:
;; https://github.com/technomancy/leiningen/issues/1544
(when-let [[resource] (-> (.getContextClassLoader (Thread/currentThread))
                          (.getResources "leiningen/release.clj")
                          (enumeration-seq) (distinct) (rest) (seq))]
  (let [release-str (str resource)]
    (when-not (re-find #"support existing release plugin" release-str)
      (clojure.lang.Compiler/load (io/reader resource)
                                  "leiningen/release.clj" release-str))))
