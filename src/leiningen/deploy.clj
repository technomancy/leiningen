(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [clojure.java.io :as io]
            [leiningen.pom :as pom]
            [leiningen.jar :as jar]))

(defn- abort-message [message]
  (cond (re-find #"Return code is 405" message)
        (str message "\n" "Ensure you are deploying over SSL.")
        (re-find #"Return code is 401" message)
        (str message "\n" "See `lein help deploy` for an explanation of how to"
             " specify credentials.")
        :else message))

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
matching repository URLs in the ~/.lein/credentials.clj.gpg file to
avoid storing plaintext credentials on your machine.

  {#\"https://internal.repo/.*\"
    {:username \"milgrim\" :password \"locative\"}
   \"s3p://s3-repo-bucket/releases\"
    {:username \"AKIAIN...\" :password \"1TChrGK4s...\"}}}}"
  ([project repository-name]
     (doseq [key [:description :license :url]]
       (when (or (nil? (project key)) (re-find #"FIXME" (str (project key))))
         (main/info "WARNING: please set" key "in project.clj.")))
     (let [jarfile (jar/jar project)
           pomfile (pom/pom project)
           repo-opts (or (get (:deploy-repositories project) repository-name)
                         (get (:repositories project) repository-name))
           repo (classpath/add-repo-auth
                 (cond
                  (not repo-opts) ["inline" {:url repository-name}]
                  (map? repo-opts) [repository-name repo-opts]
                  :else [repository-name {:url repo-opts}]))]
       (try (aether/deploy :coordinates [(symbol (:group project)
                                                 (:name project))
                                         (:version project)]
                           :jar-file (io/file jarfile)
                           :pom-file (io/file pomfile)
                           :transfer-listener :stdout
                           :repository [repo])
            (catch org.sonatype.aether.deployment.DeploymentException e
              (main/abort (abort-message (.getMessage e)))))))
  ([project]
     (deploy project (if (pom/snapshot? project)
                       "snapshots"
                       "releases"))))
