(ns leiningen.install
  "Install the project and its dependencies in your local repository."
  (:use [leiningen.jar :only [jar]]
        [leiningen.pom :only [make-model]]
        [clojure.contrib.java-utils :only [file]])
  (:import [org.apache.maven.artifact.installer ArtifactInstaller]
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

(defn make-artifact [model]
  (.createArtifactWithClassifier
   (.lookup container ArtifactFactory/ROLE)
   (.getGroupId model)
   (.getArtifactId model)
   (.getVersion model)
   (.getPackaging model)
   nil))

(defn install [project & args]
  (let [jarfile (file (jar project))
        artifact (make-artifact (make-model project))
        installer (.lookup container ArtifactInstaller/ROLE)]
    (.install installer jarfile artifact (make-local-repo))))
