(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [leiningen.core.user :as user]
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

(defn repo-for [project name]
  (let [[settings] (for [[id settings] (concat (:deploy-repositories project)
                                               (:repositories project)
                                               [[name {:url name}]])
                         :when (= id name)] settings)]
    (-> [name settings]
        (classpath/add-repo-auth)
        (add-auth-interactively))))

(defn sign [file]
  (let [exit (binding [*out* (java.io.StringWriter.)]
               (eval/sh (user/gpg-program) "--yes" "-ab" "--" file))]
    (when-not (zero? exit)
      (main/abort "Could not sign" file))
    (str file ".asc")))

(defn signature-for [extension file]
  {[:extension extension] (sign file)})

(defn signature-for-artifact [[coords artifact-file]]
  {(apply concat
          (update-in
           (apply hash-map coords) [:extension]
           #(str (or % "jar") ".asc")))
   (sign artifact-file)})

(defn sign-for-repo? [repo]
  (:sign-releases (second repo) true))

(defn files-for [project signed?]
  (let [artifacts (merge {[:extension "pom"] (pom/pom project)}
                         (jar/jar project))]
    (if (and signed? (not (.endsWith (:version project) "-SNAPSHOT")))
      (reduce merge artifacts (map signature-for-artifact artifacts))
      artifacts)))

(defn warn-missing-metadata [project]
  (doseq [key [:description :license :url]]
    (when (or (nil? (project key)) (re-find #"FIXME" (str (project key))))
      (main/info "WARNING: please set" key "in project.clj."))))

(defn deploy
  "Build jar and deploy to remote repository.

The target repository will be looked up in :repositories in project.clj:

  :repositories [[\"snapshots\" \"https://internal.repo/snapshots\"]
                 [\"releases\" \"https://internal.repo/releases\"]
                 [\"alternate\" \"https://other.server/repo\"]]

If you don't provide a repository name to deploy to, either \"snapshots\" or
\"releases\" will be used depending on your project's current version. See
`lein help deploying` under \"Authentication\" for instructions on how to
configure your credentials so you are not prompted on each deploy."
  ([project repository-name]
     (when (and (:never-deploy-snapshots project)
                (pom/snapshot? project))
       (main/abort "Cannot deploy snapshots with :never-deploy-snapshots set."))
     (warn-missing-metadata project)
     (let [repo (repo-for project repository-name)
           files (files-for project (sign-for-repo? repo))]
       (try
         (main/debug "Deploying" files "to" repo)
         (aether/deploy
          :coordinates [(symbol (:group project) (:name project))
                        (:version project)]
          :artifact-map files
          :transfer-listener :stdout
          :repository [repo])
         (catch org.sonatype.aether.deployment.DeploymentException e
           (when main/*debug* (.printStackTrace e))
           (main/abort (abort-message (.getMessage e)))))))
  ([project]
     (deploy project (if (pom/snapshot? project)
                       "snapshots"
                       "releases"))))
