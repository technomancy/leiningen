(ns leiningen.deps
  "Install jars for all dependencies in lib."
  (:require [lancet])
  (:use [leiningen.pom :only [default-repos]]
        [clojure.contrib.java-utils :only [file]])
  (:import [org.apache.maven.model Dependency Exclusion]
           [org.apache.maven.artifact.ant DependenciesTask RemoteRepository]
           [org.apache.tools.ant.util FlatFileNameMapper]))

;; TODO: unify with pom.clj

(defn make-exclusion [excl]
  (doto (Exclusion.)
    (.setGroupId (or (namespace excl) (name excl)))
    (.setArtifactId (name excl))))

(defn make-dependency [[dep version & exclusions]]
  (let [es (map make-exclusion (when (= (first exclusions) :exclusions) 
                                 (second exclusions)))]
    (doto (Dependency.)
            (.setGroupId (or (namespace dep) (name dep)))
            (.setArtifactId (name dep))
            (.setVersion version)
            (.setExclusions es))))

(defn make-repository [[id url]]
  (doto (RemoteRepository.)
    (.setId id)
    (.setUrl url)))

(defn deps
  "Download and install all :dependencies listed in project.clj into the lib/
directory. With an argument it will skip develpment dependencies. Dependencies
should be a vector of entries specifying group, name, and version like the
following:
  [org.clojure/clojure-contrib \"1.0-SNAPSHOT\"]"
  ([project skip-dev]
     (let [deps-task (DependenciesTask.)]
       (.setBasedir lancet/ant-project (:root project))
       (.setFilesetId deps-task "dependency.fileset")
       (.setProject deps-task lancet/ant-project)
       (.setPathId deps-task (:name project))
       (doseq [r (map make-repository (concat default-repos
                                              (:repositories project)))]
         (.addConfiguredRemoteRepository deps-task r))
       (doseq [dep (:dependencies project)]
         (.addDependency deps-task (make-dependency dep)))
       (when-not skip-dev
         (doseq [dep (:dev-dependencies project)]
           (.addDependency deps-task (make-dependency dep))))
       (.execute deps-task)
       (.mkdirs (file (:library-path project)))
       (lancet/copy {:todir (:library-path project) :flatten "on"}
                    (.getReference lancet/ant-project
                                   (.getFilesetId deps-task)))))
  ([project] (deps project false)))
