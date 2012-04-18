(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.classpath :as classpath]
            [clojure.java.io :as io]
            [leiningen.pom :as pom]
            [leiningen.jar :as jar]))

(defn deploy
  "Build jar and deploy to remote repository.

The target repository will be looked up in :repositories: snapshot
versions will go to the repo named \"snapshots\" while stable versions
will go to \"releases\". You can also deploy to another repository
in :repositories by providing its name as an argument, or specify
the repository URL directly.

  :repositories {\"snapshots\" \"https://internal.repo/snapshots\"
                 \"releases\" \"https://internal.repo/releases\"
                 \"alternate\" \"https://other.server/repo\"}

You should set authentication options keyed by repository URL or regex
matching repository URLs in the :auth profile in ~/.lein/profiles.clj
to avoid checking sensitive information into source control:

  {:user {:plugins [...]}
   :auth {:repository-auth {#\"https://internal.repo/.*\"
                            {:username \"milgrim\" :password \"locative\"}
                            \"s3://s3-repo-bucket/releases\"
                            {:username \"AKIAIN...\" :password \"1TChrGK4s...\"}}}}"
  ([project repository-name]
     (let [jarfile (jar/jar project)
           pomfile (pom/pom project)
           repo-opts (or (get (:deploy-repositories project) repository-name)
                         (get (:repositories project) repository-name))
           repo (classpath/add-repo-auth (cond
                                           (not repo-opts) ["inline" {:url repository-name}]
                                           (map? repo-opts) [repository-name repo-opts]
                                           :else [repository-name {:url repo-opts}]))]
       (if (number? jarfile)
         ;; if we failed to create the jar, return the status code for exit
         jarfile
         (do ;; (install-shell-wrappers (JarFile. jarfile))
           (aether/deploy :coordinates [(symbol (:group project)
                                                (:name project))
                                        (:version project)]
                          :jar-file (io/file jarfile)
                          :pom-file (io/file pomfile)
                          :transfer-listener :stdout
                          :repository [repo])
             0))))
  ([project]
     (deploy project (if (pom/snapshot? project)
                       "snapshots"
                       "releases"))))
