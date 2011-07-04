(ns leiningen.deps
  "Download all dependencies and put them in :library-path."
  (:require [lancet.core :as lancet])
  (:use [leiningen.core :only [repositories-for user-settings no-dev?]]
        [leiningen.util.maven :only [make-dependency]]
        [leiningen.util.file :only [delete-file-recursively]]
        [leiningen.util.paths :only [get-os get-arch]]
        [clojure.java.io :only [file copy]])
  (:import (java.io File)
           (java.util.jar JarFile)
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
    ;; TODO: change default to :fail in 2.0
    (.setChecksumPolicy (checksum-policies (:checksum policy-settings :warn)))
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

;; TODO: remove in 2.0; with local-repo-classpath it's unnecessary
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
      (.addDependency deps-task (make-dependency dep project)))
    deps-task))

(defn- sha1-digest [content]
  (.toString (BigInteger. 1 (-> (MessageDigest/getInstance "SHA1")
                                (.digest (.getBytes content)))) 16))

(defn- deps-checksum [project]
  (sha1-digest (pr-str [(:dependencies project)
                        (:dev-dependencies project)])))

(defn- new-deps-checksum-file [project]
  (File. (:root project) ".lein-deps-sum"))

(defn- has-dependencies? [project]
  (some (comp seq project) [:dependencies :dev-dependencies]))

(defn fetch-deps? [project]
  (let [deps-checksum-file (new-deps-checksum-file project)]
    (and (has-dependencies? project)
         (or (empty? (.list (File. (:library-path project))))
             (not (:checksum-deps project (:checksum-deps (user-settings))))
             (not (.exists deps-checksum-file))
             (not= (slurp deps-checksum-file) (deps-checksum project))))))

(defn do-deps [project deps-set]
  (let [deps-task (make-deps-task project deps-set)]
    (when (seq (deps-set project))
      (.execute deps-task)
      (when-not (and (:local-repo-classpath project)
                     (= :dependencies deps-set))
        (.mkdirs (File. (:library-path project)))
        (copy-dependencies (:jar-behavior project)
                           ;; Leiningen's process only has access to lib/dev.
                           (if (or (= :dev-dependencies deps-set)
                                   (and (:eval-in-leiningen project)
                                        (not= "leiningen" (:name project))))
                             (str (:root project) "/lib/dev")
                             (:library-path project))
                           true (.getReference lancet/ant-project
                                               (.getFilesetId deps-task)))))
    (.getReference lancet/ant-project (.getFilesetId deps-task))))

(defn- fileset-paths [fileset]
  (-> fileset
      (.getDirectoryScanner lancet/ant-project)
      (.getIncludedFiles)))

(defn- find-local-repo-jars [project]
  ;; TODO: Shut up, ant. You are useless. Nobody cares about what you say.
  ;; Removing ant-project loggers and redirecting their output streams
  ;; does nothing. How to suppress output?
  (for [path (fileset-paths (do-deps project :dependencies))]
    (file (System/getProperty "user.home") ".m2" "repository" path)))

(defn- find-lib-jars [project]
  (.listFiles (file (:library-path project))))

;; TODO: memoize when not in tests
(defn ^{:internal true} find-jars
  "Returns a seq of Files for all the jars in the project's library directory."
  [project]
  (filter #(.endsWith (.getName %) ".jar")
          (concat (if (:local-repo-classpath project)
                    (find-local-repo-jars project)
                    (find-lib-jars project))
                  ;; This must be hard-coded because it's used in
                  ;; bin/lein and thus can't be changed in project.clj.
                  (.listFiles (file (:root project) "lib/dev")))))

(def native-subdir (format "native/%s/%s/" (name (get-os)) (name (get-arch))))

(defn extract-native-deps [project]
  (doseq [jar (map #(JarFile. %) (find-jars project))
          entry (enumeration-seq (.entries jar))
          :when (.startsWith (.getName entry) native-subdir)]
    (let [f (file (:native-path project)
                  (subs (.getName entry) (count native-subdir)))]
      (if (.isDirectory entry)
        (.mkdirs f)
        (copy (.getInputStream jar entry) f)))))

(defn deps
  "Download :dependencies and put them in :library-path."
  [project & _]
  (when (seq _)
    (println "WARNING: passing an argument to deps is deprecated."))
  (when (fetch-deps? project)
    (when-not (or (:disable-deps-clean project)
                  (:disable-implicit-clean project))
      (delete-file-recursively (:library-path project) :silently)
      (delete-file-recursively (File. (:root project) "native") :silently))
    (let [fileset (do-deps project :dependencies)]
      (when-not (no-dev?)
        (do-deps project :dev-dependencies))
      (extract-native-deps project)
      (when (:checksum-deps project)
        (spit (new-deps-checksum-file project) (deps-checksum project)))
      fileset)))
