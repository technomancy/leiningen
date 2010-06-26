(ns leiningen.deps
  "Install jars for all dependencies in lib."
  (:require [lancet])
  (:use [leiningen.pom :only [default-repos make-dependency]]
        [leiningen.clean :only [empty-directory]]
        [clojure.java.io :only [file]])
  (:import [org.apache.maven.artifact.ant Authentication
                                          DependenciesTask
                                          RemoteRepository]
           org.apache.maven.settings.Server
           [org.apache.tools.ant.util FlatFileNameMapper]))

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
                :resource (.getCanonicalPath (file dir f))}))))

;; TODO: unify with pom.clj

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

(defn get-repository-list [project]
  (concat
   (if (:omit-default-repositories project)
     {}
     default-repos)
   (:repositories project)))

(defn deps
  "Download and install all :dependencies listed in project.clj.
With an argument it will skip development dependencies."
  ([project skip-dev set]
     (when-not (:disable-implicit-clean project)
       (empty-directory (:library-path project)))
     (let [deps-task (DependenciesTask.)]
       (.setBasedir lancet/ant-project (:root project))
       (.setFilesetId deps-task "dependency.fileset")
       (.setProject deps-task lancet/ant-project)
       (.setPathId deps-task (:name project))
       (doseq [r (map make-repository (get-repository-list project))]
         (.addConfiguredRemoteRepository deps-task r))
       (doseq [dep (project set)]
         (.addDependency deps-task (make-dependency dep)))
       (.execute deps-task)
       (.mkdirs (file (:library-path project)))
       (copy-dependencies (:jar-behavior project)
                          (:library-path project) true
                          (.getReference lancet/ant-project
                                         (.getFilesetId deps-task)))
       (when (and (not skip-dev) (seq (:dev-dependencies project)))
         (deps (assoc project :library-path (str (:root project) "/lib/dev"))
               true :dev-dependencies))))
  ([project skip-dev] (deps project skip-dev :dependencies))
  ([project] (deps project false)))
