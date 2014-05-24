(ns leiningen.release
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(defn parse-semantic-version [version-string]
  "Create map representing the given version string. Raise exception if the
  string does not follow guidelines setforth by Semantic Versioning 2.0.0,
  http://semver.org/"
  ;; <MajorVersion>.<MinorVersion>.<PatchVersion>[-<BuildNumber | Qualifier >]
  (let [version-map (->> (re-matches #"(\d+).(\d+).(\d+).*" version-string)
                         (drop 1)
                         (map #(Integer/parseInt %))
                         (zipmap [:major :minor :patch]))
        qualifier (->> (re-matches #".*-(.+)?" version-string)
                       (last))]
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

(defn increment-version
  "Given version as a map of the sort returned by parse-semantic-version, return
  incremented version as a map."
  ([version-map]
   (increment-version version-map (:format version-map)))
  ([version-map version-level]
   (let [{:keys [major minor patch qualifier]} version-map]
     (cond
       (= version-level :major)
       {:major (inc major)
        :minor 0
        :patch 0
        :qualifier qualifier}
       (= version-level :minor)
       {:major major
        :minor (inc minor)
        :patch 0
        :qualifier qualifier}
       (= version-level :patch)
       {:major major
        :minor minor
        :patch (inc patch)
        :qualifier qualifier}))))

(defn release
  "Bump release version, tag commit, and deploy to maven repository.

This task is intended to perform the following roughly-outlined tasks:
  * Bump version number to release.
  * Tag SCM with new version number.
  * Deploy to release repository (performs compile and jar tasks)
  * Bump version number to next snapshot.
  * Commit new version number to SCM.
"
  [project]
  (let [current-version (parse-semantic-version (:version project))
        new-dev-version (increment-version current-version)
        release-version-string (version-map->string current-version)
        new-dev-version-string (str (version-map->string new-dev-version)
                                    "-" (:qualifier new-dev-version))]
        ;;scm (->Git working-directory)]
    ;; (leiningen.change/change {:version (version-map->string release-version-string)})
    ;; (add scm "project.clj")
    ;; (commit scm (format "lein-release: preparing %s release" (release-version-string)))
    ;; (tag scm (format "%s-%s" (:name project) release-version-string))
    ;; (leiningen.deploy/deploy)
    ;; (leiningen.change/change :version new-dev-version-string)
    ;; (add scm "project.clj")
    ;; (commit scm (format "lein-release: bump version %s to %s" release-version-string new-dev-version-string))
    ;; (push scm)
    println "Release task under construction."))
