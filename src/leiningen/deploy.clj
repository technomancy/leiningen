(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether])
  (:use [leiningen.core :only [abort repositories-for]]
        [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom snapshot?]]
        [clojure.java.io :only [file]]))

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
     (let [jarfile (jar project)
           pomfile (pom project)
           repo-opts (or (get (apply hash-map (flatten (:deploy-repositories project))) repository-name)
                         (get (apply hash-map (flatten (:repositories project))) repository-name))
           repo (if repo-opts
                  {repository-name repo-opts}
                  {"inline" repository-name})]
       (if (number? jarfile)
         ;; if we failed to create the jar, return the status code for exit
         jarfile
         (do ;; (install-shell-wrappers (JarFile. jarfile))
           (aether/deploy :coordinates [(symbol (:group project)
                                                (:name project))
                                        (:version project)]
                          :jar-file (file jarfile)
                          :pom-file (file pomfile)
                          :repository repo)
             0))))
  ([project]
     (deploy project (if (snapshot? project)
                       "snapshots"
                       "releases"))))
