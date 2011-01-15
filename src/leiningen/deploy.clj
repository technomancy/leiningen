(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [lancet])
  (:use [leiningen.core :only [abort]]
        [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom]]
        [leiningen.util.maven :only [make-model make-artifact]]
        [leiningen.deps :only [make-repository make-auth]]
        [clojure.java.io :only [file]])
  (:import [org.apache.maven.artifact.ant DeployTask Pom Authentication]
           [org.apache.maven.project MavenProject]))

(defn- make-maven-project [project]
  (doto (MavenProject. (make-model project))
    (.setArtifact (make-artifact (make-model project)))))

;; for supporting command-line options
(defn- keywordize-opts [options]
  (let [options (apply hash-map options)]
    (zipmap (map keyword (keys options)) (vals options))))

(defn make-target-repo [repo-url auth-options]
  (let [repo (make-repository ["remote repository" repo-url])]
    (when-let [auth (make-auth repo-url auth-options)]
      (.addAuthentication repo auth))
    repo))

(defn deploy
  "Build and deploy jar to remote repository. Takes target repository
URL as an argument or set :deploy-to in project.clj to a URL or auth vector:

  [\"http://secret.com/archiva/repository/snapshots/\"
   :username \"durin\" :password \"mellon\"].

Also supported are :private-key and :passphrase. You can set
authentication options in ~/.lein/init.clj as well to avoid checking
sensitive information into source control:

  (def leiningen-auth {\"http://secr.et/repo\" {:password \"reindeerflotilla\"}
                       \"file:///var/repo {:passphrase \"vorpalbunny\"}})"
  ([project repo-url & auth]
     (doto (DeployTask.)
       (.setProject lancet/ant-project)
       (.getSupportedProtocols) ;; see note re: exceptions in deps.clj
       (.setFile (file (jar project)))
       (.addPom (doto (Pom.)
                  (.setMavenProject (make-maven-project project))
                  (.setFile (file (pom project)))))
       (.addRemoteRepository (make-target-repo repo-url (keywordize-opts auth)))
       (.execute)))
  ([project]
     (when-not (:deploy-to project)
       (abort "Can't deploy without :deploy-to set in project.clj."))
     (if (string? (:deploy-to project))
       (deploy project (:deploy-to project))
       (apply project (:deploy-to project)))))
