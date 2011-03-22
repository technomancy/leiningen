(ns leiningen.deps
  "Download all dependencies and place them in the :library-path."
  (:require [lancet.core :as lancet])
  (:use [leiningen.core :only [repositories-for]]
        [leiningen.util.maven :only [make-dependency]]
        [leiningen.util.file :only [delete-file-recursively]])
  (:import (java.io File)
           (java.security MessageDigest)
           (org.apache.maven.artifact.ant Authentication DependenciesTask
                                          RemoteRepository RepositoryPolicy)
           (org.apache.maven.settings Server)
           (org.apache.maven.artifact.repository ArtifactRepositoryPolicy)
           (org.apache.tools.ant.util FlatFileNameMapper)))

(def update-policies {:daily ArtifactRepositoryPolicy/UPDATE_POLICY_DAILY
                      :always ArtifactRepositoryPolicy/UPDATE_POLICY_ALWAYS
                      :never ArtifactRepositoryPolicy/UPDATE_POLICY_NEVER})

(def checksum-policies {:fail ArtifactRepositoryPolicy/CHECKSUM_POLICY_FAIL
                        :ignore ArtifactRepositoryPolicy/CHECKSUM_POLICY_IGNORE
                        :warn ArtifactRepositoryPolicy/CHECKSUM_POLICY_WARN})

(defn- make-policy [policy-settings enabled?]
  (doto (RepositoryPolicy.)
    (.setUpdatePolicy (update-policies (:update policy-settings :daily)))
    (.setChecksumPolicy (checksum-policies (:checksum policy-settings :fail)))
    (.setEnabled (boolean enabled?))))

(defn- set-policies [repo {:keys [snapshots releases] :as settings}]
  (.addSnapshots repo (make-policy snapshots (:snapshots settings true)))
  (.addReleases repo (make-policy releases (:releases settings true))))

(defn make-auth [settings]
  (let [user-options (when-let [user-opts (resolve 'user/leiningen-auth)]
                       (get @user-opts (:url settings)))
        {:keys [username password passphrase
                private-key] :as settings} (merge user-options settings)
        auth (Authentication.)]
    (when (seq settings)
      (when username (.setUserName auth username))
      (when password (.setPassword auth password))
      (when passphrase (.setPassphrase auth passphrase))
      (when private-key (.setPrivateKey auth private-key))
      auth)))

(defn make-repository [[id settings]]
  (let [repo (RemoteRepository.)]
    (set-policies repo settings)
    (.setId repo id)
    (.setUrl repo (:url settings))
    (when-let [auth (make-auth settings)]
      (.addAuthentication repo auth))
    repo))

(defn make-repositories [project]
  (map make-repository (repositories-for project)))

;; Add symlinking to Lancet's toolbox.
(lancet/define-ant-task symlink symlink)

(defmulti copy-dependencies (fn [k destination flatten? fileset] k))

(defmethod copy-dependencies :default [k destination flatten? fileset]
  (lancet/copy {:todir destination :flatten (if flatten? "on" "off")}
               fileset))

(defmethod copy-dependencies :symlink [k destination flatten? fileset]
  (let [files (.getIncludedFiles
               (.getDirectoryScanner fileset lancet/ant-project))
        dir (.getDir fileset)]
    ;; In principle, this should work... but it doesn't.
    ;; Instead we link each file in turn.
    #_(symlink {:action "record" :linkfilename destination}
               fileset)
    (doseq [f files]
      (symlink {:link destination
                :resource (.getCanonicalPath (File. dir f))}))))

(defn make-deps-task [project deps-set]
  (let [deps-task (DependenciesTask.)]
    (.setProject deps-task lancet/ant-project)
    ;; in maven-ant-tasks (at least 2.0.10 and 2.1.1) if there's an
    ;; exception thrown, there must be a call to
    ;; AbstractArtifactTask.getContainer() made to set some local
    ;; state on the task, before the exception happens, or else you
    ;; don't see stack traces. getContainer is a protected
    ;; method. Since we don't have contrib, we can't use
    ;; wall-hack-method, and clojure.lang.Reflector doesn't call
    ;; private methods, we'll call a public method that we know calls
    ;; getContainer, getSupportedProtocols.
    (.getSupportedProtocols deps-task)
    (.setBasedir lancet/ant-project (:root project))
    (.setFilesetId deps-task "dependency.fileset")
    (.setPathId deps-task (:name project))
    (doseq [repo (make-repositories project)]
      (.addConfiguredRemoteRepository deps-task repo))
    (doseq [dep (project deps-set)]
      (.addDependency deps-task (make-dependency project dep)))
    deps-task))

(defn use-dev-deps? [project skip-dev]
  (and (not skip-dev) (seq (:dev-dependencies project))))

(defn- sha1-digest [content]
  (.toString (BigInteger. 1 (-> (MessageDigest/getInstance "SHA1")
                                (.digest (.getBytes content)))) 16))

(defn- deps-checksum [project]
  (sha1-digest (pr-str [(:dependencies project)
                        (:dev-dependencies project)])))

(defn- new-deps-checksum-file [project]
  (File. (:root project) ".lein-deps-sum"))

(defn fetch-deps? [project deps-set skip-dev]
  (let [deps-checksum-file (new-deps-checksum-file project)]
    (and (or (seq (project deps-set)) (use-dev-deps? project skip-dev))
         (or (not (:checksum-deps project))
             (empty? (.list (File. (:library-path project))))
             (not (.exists deps-checksum-file))
             (not= (slurp deps-checksum-file) (deps-checksum project))))))

(defn ^{:help-arglists '([] [skip-dev])} deps
  "Download and install all :dependencies and :dev-dependencies listed in
project.clj. With an argument it will skip development dependencies."
  ([project skip-dev deps-set]
     (when (fetch-deps? project deps-set skip-dev)
       (when-not (or (:disable-deps-clean project)
                     (:disable-implicit-clean project))
         (delete-file-recursively (:library-path project) :silently))
       (let [deps-task (doto (make-deps-task project deps-set) .execute)
             fileset (.getReference lancet/ant-project
                                    (.getFilesetId deps-task))]
         (.mkdirs (File. (:library-path project)))
         (copy-dependencies (:jar-behavior project)
                            (:library-path project)
                            true fileset)
         (when (use-dev-deps? project skip-dev)
           (deps (assoc project :library-path (str (:root project) "/lib/dev"))
                 true :dev-dependencies))
         (when (:checksum-deps project)
           (spit (new-deps-checksum-file project) (deps-checksum project)))
         fileset)))
  ([project skip-dev] (deps project skip-dev :dependencies))
  ([project] (deps project false)))
