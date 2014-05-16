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

(defn stringify-version-map
  "Given a version-map, return a proper string of the :version array."
  [version-map]
  (string/join "." (:version version-map)))

(defn inc-nth-zero-rest
  "Increment nth integer in number-vector, zero all the rest."
  [n number-vector]
  (let [split-vector (split-at n number-vector)
        version (first split-vector)
        zeros (second split-vector)]
    (concat (butlast version) (inc (last version)) (repeat (count zeros) 0))))

(defn increment-version
  "Given old-version as a string, return incremented version as a map. Version
  level to be incremented is determined by version-level."
  [old-version version-level]
  (let [version-map (parse-maven-version old-version)]
    (if (not (>= (count (:version version-map))
                 (version-level maven-version-indices)))
      (throw (Exception.
               (format "%s does not have a %s version level."
                       old-version
                       (name version-level)))))
    (if (= (:format version-map) :not-recognized)
      (throw (Exception. "Unrecognized Maven version string."))
      (assoc! version-map :version (inc-nth-zero-rest
                                     (version-level maven-version-indices))))))

(defn compute-next-version
  "Computes the next leiningen version string based on what is given."
  [old-version version-level release-type]
  (let [new-version-map (increment-version old-version version-level)]
    (when
      (= release-type :snapshot)
      (str (stringify-version-map new-version-map) "-SNAPSHOT")

      (= release-type :release)
      (stringify-version-map new-version-map)

      :else
      (throw (Exception.
               (format ":%s is not a proper release type!"
                       (name release-type)))))))

(defn release
  "Bump release version, tag commit, and deploy to maven repository.

This task is intended to perform the following roughly-outlined tasks:
  * Bump version number to release.
  * Tag SCM with new version number.
  * Deploy to release repository (performs compile and jar tasks)
  * Bump version number to next snapshot.
  * Commit new version number to SCM.
"
  ([project]
   (release :snapshot))
  ([project version-level]
   (let [current-version (:version project)
         release-version (compute-next-version current-version version-level :release)
         new-dev-version (compute-next-version release-version version-level :snapshot)]
         ;;scm (->Git working-directory)]
    ;; (leiningen.change/change :version release-version)
    ;; (add scm "project.clj")
    ;; (commit scm (format "lein-release: preparing %s release" release-version))
    ;; (tag scm (format "%s-%s" (:name project) release-version))
    ;; (leiningen.deploy/deploy)
    ;; (leiningen.change/change :version new-dev-version)
    ;; (add scm "project.clj")
    ;; (commit scm (format "lein-release: bump version %s to %s" release-version new-dev-version))
    ;; (push scm)
    println "Release task under construction.")))
