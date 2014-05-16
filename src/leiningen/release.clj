(ns leiningen.release
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(defprotocol SCM
  (add    [this files] "Add changes to specified file to SCM index.")
  (commit [this message] "Commit specified changes to SCM repository.")
  (push   [this remote-repository remote-branch] "Push recent changes to upstream repository.")
  (status [this] "Display working directory SCM status.")
  (tag    [this commit commit-name] "Name commit with "))

(defrecord Git [working-dir git-dir])

(def maven-version-regexes
     {:major-only                               #"(\d+)(?:-(.+))?"
      :major-and-minor                          #"(\d+)\.(\d+)(?:-(.+))?"
      :major-minor-and-incremental              #"(\d+)\.(\d+)\.(\d+)(?:-(.+))?"})

(def maven-version-indices {:major 1 :minor 2 :incremental 3})

(defn parse-maven-version [version-string]
  "Create map representing the given version string."
  ;; <MajorVersion [> . <MinorVersion [> . <IncrementalVersion ] ] [> - <BuildNumber | Qualifier ]>
  (cond
    (re-matches (:major-only maven-version-regexes) version-string)
    (let [[[_ major qualifier]]
          (re-seq (:major-only maven-version-regexes) version-string)]
      {:format      :major-only
       :version     (map edn/read-string [major])
       :qualifier   qualifier})

    (re-matches (:major-and-minor maven-version-regexes) version-string)
    (let [[[_ major minor qualifier]]
          (re-seq (:major-and-minor maven-version-regexes) version-string)]
      {:format      :major-and-minor
       :version     (map edn/read-string [major minor])
       :qualifier   qualifier})

    (re-matches (:major-minor-and-incremental maven-version-regexes) version-string)
    (let [[[_ major minor incremental qualifier]]
          (re-seq (:major-minor-and-incremental maven-version-regexes) version-string)]
      {:format      :major-minor-and-incremental
       :version     (map edn/read-string [major minor incremental])
       :qualifier   qualifier})

    :else
    {:format :not-recognized
     :version version-string}))

(defn version-map->string
  "Given a version-map, return a proper string of the :version array."
  [version-map]
  (string/join "." (:version version-map)))

(defn increment-version
  "Given version as a map of the sort returned by parse-maven-version, return
  incremented version as a map. Always assume version level to be incremented is
  the least significant position, ie incremental if it's available, minor if
  not, major if neither incremental nor minor are available. Let user manually
  adjust major/minor if necessary."
  [version-map]
  (if (= (:format version-map) :not-recognized)
    (throw (Exception. "Unrecognized Maven version string."))
    (assoc! version-map :version (conj (butlast (:version version-map)) 0))))

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
  (let [current-version (parse-maven-version (:version project))
        new-dev-version (increment-version current-version)
        kelease-version-string (version-map->string current-version)
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
    println "Release task under construction.")))
