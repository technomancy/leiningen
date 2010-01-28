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
directory. With an argument it will skip development dependencies. Dependencies
should be a vector of entries specifying group, name, and version like the
following:
  [org.clojure/clojure-contrib \"1.0-SNAPSHOT\"]

It is also possible to exclude specific indirect dependencies of a direct
dependency using the optional :exclusions keyword and vector of entries.
A project that depends on log4j could exclude unnecessary indirect
dependencies with the following:
  [log4j \"1.2.15\" :exclusions [javax.mail/mail
                                 javax.jms/jms
                                 com.sun.jdmk/jmxtools
                                 com.sun.jmx/jmxri]]"
  ;; TODO: get deps from ~/.m2 while offline
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
