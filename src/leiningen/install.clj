(ns leiningen.install
  "Install the project and its dependencies in your local repository."
  (:use [leiningen.jar :only [jar manifest-map local-repo-path]]
        [leiningen.pom :only [pom make-model default-repos]]
        [leiningen.core :only [home-dir]]
        [clojure.java.io :only [file copy]])
  (:import [org.apache.maven.artifact.installer ArtifactInstaller]
           [org.apache.maven.project.artifact ProjectArtifactMetadata]
           [org.apache.maven.settings MavenSettingsBuilder]
           [org.apache.maven.artifact.repository ArtifactRepositoryFactory
            DefaultArtifactRepository]
           [org.apache.maven.artifact.factory ArtifactFactory]
           [org.apache.maven.artifact.repository ArtifactRepositoryPolicy]
           [org.apache.maven.artifact.repository.layout
            ArtifactRepositoryLayout]
           [java.util.jar JarFile]
           [org.apache.maven.artifact.resolver ArtifactResolver]
           [org.codehaus.plexus.embed Embedder]))

;; Welcome to the absurdist self-parodying world of Dependency Injection
(def container (.getContainer (doto (Embedder.) (.start))))

(def layout (.lookup container ArtifactRepositoryLayout/ROLE "default"))

(def policy
     (ArtifactRepositoryPolicy. true
                                ArtifactRepositoryPolicy/UPDATE_POLICY_DAILY
                                ArtifactRepositoryPolicy/CHECKSUM_POLICY_FAIL))

(defn make-settings []
  (.buildSettings (.lookup container MavenSettingsBuilder/ROLE)))

(defn make-local-repo []
  (let [path (.getLocalRepository (make-settings))
        url (if (.startsWith path "file:") path (str "file://" path))]
    (-> (.lookup container ArtifactRepositoryFactory/ROLE)
        (.createDeploymentArtifactRepository
         "local" url layout true))))

(defn make-remote-repo [[name url]]
  (-> (.lookup container ArtifactRepositoryFactory/ROLE)
      (.createArtifactRepository
       name url layout policy policy)))

(defn add-metadata [artifact pomfile]
  (.addMetadata artifact (ProjectArtifactMetadata. artifact pomfile)))

(defn make-artifact [model]
  (.createArtifactWithClassifier
   (.lookup container ArtifactFactory/ROLE)
   (.getGroupId model)
   (.getArtifactId model)
   (.getVersion model)
   (.getPackaging model)
   nil))

(defn bin-path []
  (doto (file (home-dir) "bin") .mkdirs))

(defn install-shell-wrapper [jarfile]
  (when-let [bin-name ((manifest-map (.getManifest jarfile))
                       "Leiningen-shell-wrapper")]
    (let [bin-file (file (bin-path) (last (.split bin-name "/")))]
      (println "Installing shell wrapper to" (.getAbsolutePath bin-file))
      (copy (.getInputStream jarfile (.getEntry jarfile bin-name)) bin-file)
      (.setExecutable bin-file true))))

(defn make-remote-artifact [name group version]
  (.createArtifact
   (.lookup container ArtifactFactory/ROLE)
   (or group name) name
   version "compile" "jar"))

(defn standalone-install [name group version]
  (let [resolver (.lookup container ArtifactResolver/ROLE)
        artifact (make-remote-artifact name group version)
        remote-repos (map make-remote-repo default-repos)
        local-repo (make-local-repo)]
    (.resolveAlways resolver artifact remote-repos local-repo)
    (println (local-repo-path name group version))
    (-> (local-repo-path name group version)
        file
        JarFile.
        install-shell-wrapper)))

(defn install
  "Install the project and its dependencies in your local repository."
  ([project]
     (let [jarfile (file (jar project))
           model (make-model project)
           artifact (make-artifact model)
           installer (.lookup container ArtifactInstaller/ROLE)
           local-repo (make-local-repo)]
       ;; for packaging other than "pom" there should be "pom.xml"
       ;; generated and installed in local repo
       (if (not= "pom" (.getPackaging model))
         (add-metadata artifact (file (pom project))))
       (install-shell-wrapper (JarFile. jarfile))
       (.install installer jarfile artifact local-repo)))
  ([_ project-name version]
     (let [[name group] ((juxt name namespace) (symbol project-name))]
       (standalone-install name (or group name) version))))
