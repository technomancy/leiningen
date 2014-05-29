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
            [leiningen.clean :as clean]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(defn- abort-message [message]
  (cond (re-find #"Return code is 405" message)
        (str message "\n" "Ensure you are deploying over SSL.")
        (re-find #"Return code is 401" message)
        (str message "\n" "See `lein help deploying` for an explanation of how"
             " to specify credentials.")
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
  (if (or (and (:username settings) (some settings [:password :passphrase
                                                    :private-key-file]))
          (re-find #"(file|scp|scpexe)://" (:url settings)))
    [id settings]
    (do
      (when @utils/rebound-io?
        (main/abort "No credentials found for" id "(did you mean `lein deploy clojars`?)"
                    "\nPassword prompts are not supported when ran after other"
                    "(potentially)\ninteractive tasks. Maybe setting up"
                    "credentials may be an idea?\n\nSee `lein help deploy` for an"
                    "explanation of how to specify credentials."))
      (print "No credentials found for" id)
      (when (not= "clojars" id)
        (print " (did you mean `lein deploy clojars`?)"))
      (println "\nSee `lein help deploying` for how to configure credentials to avoid prompts.")
      (print "Username: ") (flush)
      (let [username (read-line)
            password (.readPassword (System/console) "%s"
                                    (into-array ["Password: "]))]
        [id (assoc settings :username username :password password)]))))

;; repo names must not contain path delimiters because they're used by
;; aether for form filenames
(defn- sanitize-repo-name [name]
  (last (.split name "/")))

(defn ^:internal get-aliased-repo [project name]
  (let [repositories (into {} (:repositories project))
        deploy-repositories (into {} (:deploy-repositories project))
        merged-repos (merge repositories deploy-repositories)
        ;; To support stuff like :deploy-repositories {"releases" "clojars"}
        aliased-name (if (merged-repos (merged-repos name))
                       (merged-repos name)
                       name)]
    [aliased-name (merge (repositories aliased-name)
                         (deploy-repositories aliased-name))]))

(defn repo-for [project name]
  (let [[name settings] (get-aliased-repo project name)]
    (-> [(sanitize-repo-name name) (or settings {:url name})]
        (classpath/add-repo-auth)
        (add-auth-from-url)
        (add-auth-interactively))))

(defn signing-args
  "Produce GPG arguments for signing a file."
  [file opts]
  (let [key-spec (if-let [key (:gpg-key opts)]
                   ["--default-key" key])]
    `["--yes" "-ab" ~@key-spec "--" ~file]))

(defn sign
  "Create a detached signature and return the signature file name."
  [file opts]
  (let [{:keys [err exit]} (apply user/gpg (signing-args file opts))]
    (when-not (zero? exit)
      (main/abort "Could not sign"
                  (str file "\n" err
                       "\n\nSee `lein help gpg` for how to set up gpg.\n"
                       "If you don't expect people to need to verify the "
                       "authorship of your jar, you\ncan add `:sign-releases "
                       "false` to the relevant `:deploy-repositories` entry.")))
    (str file ".asc")))

(defn signature-for-artifact [[coords artifact-file] opts]
  {(apply concat
          (update-in
           (apply hash-map coords) [:extension]
           #(str (or % "jar") ".asc")))
   (sign artifact-file opts)})

(defn sign-for-repo?
  "Generally sign artifacts for this repo?"
  [repo]
  (:sign-releases (second repo) true))

(defn signing-opts
  "Extract signing options map from a project."
  [project repo]
  (merge (:signing project) (:signing (second repo))))

(defn files-for [project repo]
  (let [signed? (sign-for-repo? repo)
        artifacts (merge {[:extension "pom"] (pom/pom project)}
                         (jar/jar project))
        sig-opts (signing-opts project repo)]
    (if (and signed? (not (.endsWith (:version project) "-SNAPSHOT")))
      (reduce merge artifacts (map #(signature-for-artifact % sig-opts)
                                   artifacts))
      artifacts)))

(defn warn-missing-metadata [project]
  (doseq [key [:description :license :url]]
    (when (or (nil? (project key)) (re-find #"FIXME" (str (project key))))
      (main/warn "WARNING: please set" key "in project.clj."))))

(defn- in-branches [branches]
  (-> (sh/sh "git" "rev-parse" "--abbrev-ref" "HEAD")
      :out
      butlast
      str/join
      branches
      not))

(defn ^:no-project-needed deploy
  "Deploy jar and pom to remote repository.

The target repository will be looked up in :repositories in project.clj:

  :repositories [[\"snapshots\" \"https://internal.repo/snapshots\"]
                 [\"releases\" \"https://internal.repo/releases\"]
                 [\"alternate\" \"https://other.server/repo\"]]

If you don't provide a repository name to deploy to, either \"snapshots\" or
\"releases\" will be used depending on your project's current version. You may
provide a repository URL instead of a name.

See `lein help deploying` under \"Authentication\" for instructions on
how to configure your credentials so you are not prompted on each
deploy.

You can also deploy arbitrary artifacts from disk:

    $ lein deploy myrepo com.blueant/fancypants 1.0.1 fancypants.jar pom.xml

While this works with any arbitrary files on disk, downstream projects will not
be able to depend on jars that are deployed without a pom."
  ([project]
     (deploy project (if (pom/snapshot? project)
                       "snapshots"
                       "releases")))
  ([project repository]
     (let [branches (set (:deploy-branches project))]
       (when (and (seq branches)
                  (in-branches branches))
         (apply main/abort "Can only deploy from branches listed in"
                ":deploy-branches:" branches)))
     (warn-missing-metadata project)
     (when (:auto-clean project true)
       (clean/clean project))
     (let [repo (repo-for project repository)
           files (files-for project repo)]
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
  ([project repository identifier version & files]
     (let [identifier (symbol identifier)
           artifact-id (name identifier)
           group-id (namespace identifier)
           repo (repo-for project repository)
           artifacts (for [f files]
                       [[:extension (if (= "pom.xml" (.getName (io/file f)))
                                      "pom" (last (.split f "\\.")))] f])]
       (main/debug "Deploying" files "to" repo)
       (aether/deploy
        :coordinates [(symbol group-id artifact-id) version]
        :artifact-map (into {} artifacts)
        :transfer-listener :stdout
        :repository [repo]
        :local-repo (:local-repo project)))))
