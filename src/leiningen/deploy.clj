(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [lancet.core :as lancet])
  (:use [leiningen.core :only [abort repositories-for]]
        [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom snapshot?]]
        [leiningen.util.maven :only [make-model make-artifact]]
        [leiningen.deps :only [make-repository]]
        [clojure.java.io :only [file]])
  (:import (org.apache.maven.artifact.ant DeployTask Pom Authentication)
           (org.apache.tools.ant BuildException)
           (org.apache.maven.project MavenProject)))

(defn- make-maven-project [project]
  (doto (MavenProject. (make-model project))
    (.setArtifact (make-artifact (make-model project)))))

(defn- get-repository [project repository-name]
  (let [repositories (repositories-for project)
        repository (or (repositories repository-name)
                       {:url repository-name})]
    (make-repository [repository-name repository])))

(defn deploy
  "Build jar and deploy to remote repository.

The target repository will be looked up in :repositories: snapshot
versions will go to the repo named \"snapshots\" while stable versions
will go to \"releases\". You can also deploy to another repository
in :repositories by providing its name as an argument.

  :repositories {\"java.net\" \"http://download.java.net/maven/2\"
                 \"snapshots\" {:url \"https://blueant.com/archiva/snapshots\"
                                :username \"milgrim\" :password \"locative\"}
                 \"releases\" {:url \"https://blueant.com/archiva/internal\"
                               :private-key \"etc/id_dsa\"}}

You can set authentication options keyed by repository name in
~/.lein/init.clj to avoid checking sensitive information into source
control:

  (def leiningen-auth {\"https://blueant.com/archiva/internal\"
                       {:passphrase \"vorpalbunny\"}})
"
  ([project repository-name]
     (try (doto (DeployTask.)
            (.setProject lancet/ant-project)
            (.getSupportedProtocols) ;; see note re: exceptions in deps.clj
            (.setFile (file (jar project)))
            (.addPom (doto (Pom.)
                       (.setMavenProject (make-maven-project project))
                       (.setFile (file (pom project)))))
            (.addRemoteRepository (get-repository project repository-name))
            (.execute))
          (catch BuildException _ 1)))
  ([project]
     (deploy project (if (snapshot? project)
                       "snapshots"
                       "releases"))))
