(ns leiningen.deploy
  "Build and deploy jar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [leiningen.core.user :as user]
            [leiningen.core.utils :as utils]
            [clojure.java.io :as io]
            [leiningen.pom :as pom]
            [leiningen.jar :as jar]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(defn- abort-message [message]
  (cond (re-find #"Return code is 405" message)
        (str message "\n" "Ensure you are deploying over SSL.")
        (re-find #"Return code is 401" message)
        (str message "\n" "See `lein help deploy` for an explanation of how to"
             " specify credentials.")
        :else message))

(defn add-auth-from-url
  [[id settings]]
  (let [url (utils/build-url id)
        user-info (and url (.getUserInfo url))
        [username password] (and user-info (.split user-info ":"))]
    (if username
      [id (assoc settings :username username :password password)]
      [id settings])))

(defn add-auth-interactively [[id settings]]
  (cond
   (or (and (:username settings) (some settings [:password :passphrase
                                                 :private-key-file]))
       (.startsWith (:url settings) "file://"))
   [id settings]

   @utils/rebound-io?
   (main/abort "No credentials found for" id
               "\nPassword prompts are not supported when ran after other"
               "(potentially)\ninteractive tasks. Maybe setting up credentials"
               "may be an idea?\n\nSee `lein help gpg` for an explanation of"
               "how to specify credentials.")

   :else
   (do
     (println "No credentials found for" id)
     (println "See `lein help gpg` for how to configure credentials.")
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
        (add-auth-from-url)
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

(defn- in-branches [branches]
  (-> (sh/sh "git" "rev-parse" "--abbrev-ref" "HEAD")
      :out
      butlast
      str/join
      branches
      not))

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
     (let [branches (set (:deploy-branches project))]
       (when (and (seq branches)
                  (in-branches branches))
         (apply main/abort "Can only deploy from branches listed in :deploy-branches:" branches)))
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
