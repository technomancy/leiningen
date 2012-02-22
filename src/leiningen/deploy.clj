(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.classpath :as classpath])
  (:use [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom snapshot?]]
        [clojure.java.io :only [file]]))

(defn deploy
  "Build jar and deploy to remote repository.

The target repository will be looked up in :repositories: snapshot
versions will go to the repo named \"snapshots\" while stable versions
will go to \"releases\". You can also deploy to another repository
in :repositories by providing its name as an argument.

  :repositories {\"snapshots\" \"https://internal.repo/snapshots\"
                 \"releases\" \"https://internal.repo/releases\"
                 \"alternate\" \"https://other.server/repo\"}

You should set authentication options keyed by repository URL in
the :deploy profile in ~/.lein/profiles.clj to avoid checking
sensitive information into source control:

  {:user {:plugins [...]}
   :auth {:repository-auth {\"https://internal.repo/snapshots\"
                            {:username \"milgrim\" :password \"locative\"}}}}"
  ([project repository-name]
     (let [jarfile (jar project)
           pomfile (pom project)
           repo-opts (or (get (:deploy-repositories project) repository-name)
                         (get (:repositories project) repository-name))
           repo (classpath/add-repo-auth (if (map? repo-opts)
                                           [repository-name repo-opts]
                                           [repository-name {:url repo-opts}]))]
       (if (number? jarfile)
         ;; if we failed to create the jar, return the status code for exit
         jarfile
         (do ;; (install-shell-wrappers (JarFile. jarfile))
           (aether/deploy :coordinates [(symbol (:group project)
                                                (:name project))
                                        (:version project)]
                          :jar-file (file jarfile)
                          :pom-file (file pomfile)
                          :transfer-listener :stdout
                          :repository [repo])
             0))))
  ([project]
     (deploy project (if (snapshot? project)
                       "snapshots"
                       "releases"))))
