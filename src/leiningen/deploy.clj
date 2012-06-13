(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
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

(defn add-auth-interactively [[id settings]]
  (if (or (and (:username settings) (some settings [:password :passphrase
                                                    :private-key-file]))
          (.startsWith (:url settings) "file://"))
    [id settings]
    (do
      (println "No credentials found for" id)
      (println "See `lein help deploying` for how to configure credentials.")
      (print "Username: ") (flush)
      (let [username (read-line)
            password (.readPassword (System/console) "%s"
                                    (into-array ["Password: "]))]
        [id (assoc settings :username username :password password)]))))

(defn sign [file]
  (let [exit (binding [*out* (java.io.StringWriter.)]
               (eval/sh "gpg" "--yes" "-ab" file))]
    (when-not (zero? exit)
      (main/abort "Could not sign" file))
    (io/file (str file ".asc"))))

(defn signatures-for [jar-file pom-file]
  [[(sign jar-file) :extension "jar.asc"]
   [(sign pom-file) :extension "pom.asc"]])

(defn files-for [project repo]
  (let [jar-file (jar/jar project)
        pom-file (pom/pom project)]
    (concat [[jar-file :extension "jar"]
             [pom-file :extension "pom"]]
            (if (and (:sign-releases (second repo) true)
                     (not (.endsWith (:version project) "-SNAPSHOT")))
              (signatures-for jar-file pom-file)))))

(defn repo-for [project repository-name]
  ;; can't use merge here due to bug in ordered maps:
  ;; https://github.com/flatland/ordered/issues/4
  (let [repo-opts (or (get (:deploy-repositories project) repository-name)
                      (get (:repositories project) repository-name))
        repo (cond (not repo-opts) ["inline" {:url repository-name}]
                   (string? repo-opts) [repository-name {:url repo-opts}]
                   :else [repository-name repo-opts])
        repo (classpath/add-repo-auth repo)
        repo (add-auth-interactively repo)]
    repo))

(defn warn-missing-metadata [project]
  (doseq [key [:description :license :url]]
    (when (or (nil? (project key)) (re-find #"FIXME" (str (project key))))
      (main/info "WARNING: please set" key "in project.clj."))))

(defn deploy
  "Build jar and deploy to remote repository.

The target repository will be looked up in :repositories in project.clj:

  :repositories {\"snapshots\" \"https://internal.repo/snapshots\"
                 \"releases\" \"https://internal.repo/releases\"
                 \"alternate\" \"https://other.server/repo\"}

If you don't provide a repository name to deploy to, either \"snapshots\" or
\"releases\" will be used depending on your project's current version. See
`lein help deploying` under \"Authentication\" for instructions on how to
configure your credentials so you are not prompted on each deploy."
  ([project repository-name]
     (warn-missing-metadata project)
     (let [repo (repo-for project repository-name)
           files (files-for project repo)]
       (try
         (doseq [[file & coords] files]
           (main/info "Deploying" file coords "to" repo)
           (aether/deploy-file :coordinates (into [(symbol (:group project)
                                                           (:name project))
                                                   (:version project)] coords)
                               :file (io/file file)
                               :transfer-listener :stdout
                               :repository [repo]))
         (catch org.sonatype.aether.deployment.DeploymentException e
           (when main/*debug* (.printStackTrace e))
           (main/abort (abort-message (.getMessage e)))))))
  ([project]
     (deploy project (if (pom/snapshot? project)
                       "snapshots"
                       "releases"))))
