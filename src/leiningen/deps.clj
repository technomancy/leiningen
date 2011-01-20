(ns leiningen.deps
  "Download all dependencies and place them in the :library-path."
  (:require [lancet])
  (:use [leiningen.core :only [repositories-for]]
        [leiningen.util.maven :only [make-dependency]]
        [leiningen.util.file :only [delete-file-recursively]])
  (:import (java.io File)
           (java.security MessageDigest)
           (org.apache.maven.artifact.ant Authentication DependenciesTask
                                          RemoteRepository)
           (org.apache.maven.settings Server)
           (org.apache.tools.ant.util FlatFileNameMapper)))

(defn make-repository [[id settings]]
  (let [repo (RemoteRepository.)]
    (.setId repo id)
    (if (string? settings)
      (.setUrl repo settings)
      (let [{:keys [url username password]} settings]
        (.setUrl repo url)
        (.addAuthentication repo (Authentication. (doto (Server.)
                                                    (.setUsername username)
                                                    (.setPassword password))))))
    repo))

(defn make-auth [url options]
  (let [auth (Authentication.)
        user-options (when-let [user-opts (resolve 'user/leiningen-auth)]
                       (get @user-opts url))
        {:keys [username password passphrase
                private-key]} (merge user-options options)]
    (when username (.setUserName auth username))
    (when password (.setPassword auth password))
    (when passphrase (.setPassphrase auth passphrase))
    (when private-key (.setPrivateKey auth private-key))
    auth))

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
    (doseq [[id settings] (repositories-for project)]
      (let [r (make-repository [id settings])
            repo-url (if (string? settings) settings (:url settings))]
        (when-let [auth (make-auth repo-url (if (map? settings) settings {}))]
          (.addAuthentication r auth))
        (.addConfiguredRemoteRepository deps-task r)))
    (doseq [dep (project deps-set)]
      (.addDependency deps-task (make-dependency dep)))
    deps-task))

(defn use-dev-deps? [project skip-dev]
  (and (not skip-dev) (seq (:dev-dependencies project))))

(defn- sha1-digest [content]
  (.toString (BigInteger. 1 (-> (MessageDigest/getInstance "SHA1")
                                (.digest (.getBytes content)))) 16))

(defn- deps-checksum [project]
  (sha1-digest (pr-str [(:dependencies project)
                              (:dev-dependencies project)])))

(defn fetch-deps? [project deps-set skip-dev]
  (let [deps-checksum-file (File. (:root project) ".lein-deps-sum")]
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
       (when-not (:disable-implicit-clean project)
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
         fileset)))
  ([project skip-dev] (deps project skip-dev :dependencies))
  ([project] (deps project false)))
