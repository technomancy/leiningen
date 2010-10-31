(ns leiningen.deps
  "Install jars for all dependencies in lib."
  (:require [lancet])
  (:use [leiningen.core :only [repositories-for]]
        [leiningen.util.maven :only [make-dependency]]
        [leiningen.util.file :only [delete-file-recursively]])
  (:import [java.io File]
           [org.apache.maven.artifact.ant
            Authentication DependenciesTask RemoteRepository]
           [org.apache.maven.settings Server]
           [org.apache.tools.ant.util FlatFileNameMapper]))

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
    (doseq [r (map make-repository (repositories-for project))]
      (.addConfiguredRemoteRepository deps-task r))
    (doseq [dep (project deps-set)]
      (.addDependency deps-task (make-dependency dep)))
    deps-task))

(defn use-dev-deps? [project skip-dev]
  (and (not skip-dev) (seq (:dev-dependencies project))))

(defn deps
  "Download and install all :dependencies listed in project.clj.
With an argument it will skip development dependencies."
  ([project skip-dev deps-set]
     (when (or (seq (project deps-set)) (use-dev-deps? project skip-dev))
       (when-not (:disable-implicit-clean project)
         (delete-file-recursively (:library-path project) true))
       (let [deps-task (make-deps-task project deps-set)
             _ (.execute deps-task)
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
