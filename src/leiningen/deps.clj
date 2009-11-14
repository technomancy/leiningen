(ns leiningen.deps
  (:require [lancet])
  (:use [clojure.contrib.java-utils :only [file]])
  (:import [org.apache.maven.model Dependency]
           [org.apache.maven.artifact.ant DependenciesTask RemoteRepository]
           [org.apache.tools.ant.util FlatFileNameMapper]))

(defn make-repository [[id url]]
  (doto (RemoteRepository.)
    (.setId id)
    (.setUrl url)))

(def default-repos {"central" "http://repo1.maven.org/maven/"
                    "clojure-snapshots" "http://build.clojure.org/snapshots"})

(defn- make-dependency [[dep version]]
  (doto (Dependency.)
    (.setGroupId (or (namespace dep) (name dep)))
    (.setArtifactId (name dep))
    (.setVersion version)))

;; TODO: development dependencies
(defn deps
  "Install dependencies in lib/"
  [project & args]
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
    (.execute deps-task)
    (.mkdirs (file (:root project) "lib"))
    (lancet/copy {:todir (str (:root project) "/lib/") :flatten "on"}
                 (.getReference lancet/ant-project
                                (.getFilesetId deps-task)))))
