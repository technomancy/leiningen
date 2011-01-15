(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [lancet])
  (:use [leiningen.core :only [abort]]
        [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom snapshot?]]
        [leiningen.util.maven :only [make-model make-artifact]]
        [leiningen.deps :only [make-repository]]
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

(defn make-auth [url options]
  (let [auth (Authentication.)
        user-options (when-let [user-opts (resolve 'user/leiningen-auth)]
                       (get @user-opts url))
        {:keys [username password passphrase
                private-key]} (merge user-options options)]
    (when username (.setUserName auth username))
    (when password (.setPassword auth password))
    (when passphrase (.setPassphrase auth passphrase))
    (when private-key (.setPrivateKey auth private-key))
    auth))

(defn make-target-repo [project options]
  (let [deploy-opts (merge (:deploy-to project) options)
        repo-url (if (snapshot? project)
                   (:snapshots deploy-opts)
                   (:releases deploy-opts))
        repo (make-repository ["remote repository" repo-url])]
    (when-let [auth (make-auth repo-url options)]
      (.addAuthentication repo auth))
    repo))

(defn deploy
  "Build and deploy jar to remote repository. Set :deploy-to in project.clj:

  {:snapshots \"http://secret.com/archiva/repository/snapshots\"
   :releases \"http://secret.com/archiva/repository/internal\"
   :username \"durin\" :password \"mellon\"}

SNAPSHOT versions will be deployed to :snapshots repository, releases go to
:releases. Also supported are :private-key and :passphrase. You can
set authentication options keyed by repository URL in ~/.lein/init.clj
to avoid checking sensitive information into source control:

  (def leiningen-auth {\"http://secr.et/repo\" {:password \"reindeerflotilla\"}
                       \"file:///var/repo {:passphrase \"vorpalbunny\"}})"
  ([project & opts]
     (doto (DeployTask.)
       (.setProject lancet/ant-project)
       (.getSupportedProtocols) ;; see note re: exceptions in deps.clj
       (.setFile (file (jar project)))
       (.addPom (doto (Pom.)
                  (.setMavenProject (make-maven-project project))
                  (.setFile (file (pom project)))))
       (.addRemoteRepository (make-target-repo project (keywordize-opts opts)))
       (.execute)))
  ([project]
     (if-let [target (:deploy-to project)]
       (deploy target)
       (do (println "Either set :deploy-to in project.clj or"
                    "provide deploy target options.") 1))))
