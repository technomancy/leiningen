(ns leiningen.install
  "Install the project and its dependencies in your local repository."
  (:use [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom make-model]]
        [clojure.java.io :only [file]])
  (:import [org.apache.maven.artifact.installer ArtifactInstaller]
           [org.apache.maven.project.artifact ProjectArtifactMetadata]
           [org.apache.maven.settings MavenSettingsBuilder]
           [org.apache.maven.artifact.repository ArtifactRepositoryFactory]
           [org.apache.maven.artifact.factory ArtifactFactory]
           [org.apache.maven.artifact.repository.layout
            ArtifactRepositoryLayout]
           [org.codehaus.plexus.embed Embedder]))

;; Welcome to the absurdist self-parodying world of Dependency Injection
(def container (.getContainer (doto (Embedder.) (.start))))

(defn make-settings []
  (.buildSettings (.lookup container MavenSettingsBuilder/ROLE)))

(defn make-local-repo []
  (let [path (.getLocalRepository (make-settings))
        url (if (.startsWith path "file:") path (str "file://" path))]
    (-> (.lookup container ArtifactRepositoryFactory/ROLE)
        (.createDeploymentArtifactRepository
         "local" url (.lookup container ArtifactRepositoryLayout/ROLE "default")
         true))))

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

(defn install
  "Install the project and its dependencies in your local repository."
  [project]
  (let [jarfile (file (jar project))
        model (make-model project)
        artifact (make-artifact model)
        installer (.lookup container ArtifactInstaller/ROLE)
        local-repo (make-local-repo)]
    ;; for packaging other than "pom" there should be "pom.xml"
    ;; generated and installed in local repo
    (if (not= "pom" (.getPackaging model))
      (add-metadata artifact (file (pom project))))
    (.install installer jarfile artifact local-repo)))

