(ns leiningen.pom
  (:use [clojure.contrib.duck-streams :only [writer]]
        [clojure.contrib.java-utils :only [file]])
  (:import [org.apache.maven.model Model Parent Dependency Repository]
           [org.apache.maven.project MavenProject]))

(defn- make-dependency [[dep version]]
  (doto (Dependency.)
    (.setGroupId (or (namespace dep) (name dep)))
    (.setArtifactId (name dep))
    (.setVersion version)))

(defn- make-repository [[id url]]
  (doto (Repository.)
    (.setId id)
    (.setUrl url)))

(def default-repos {"central" "http://repo1.maven.org/maven/"
                    "clojure-snapshots" "http://build.clojure.org/snapshots"})

(defn make-model [project]
  (let [model (doto (Model.)
                (.setModelVersion "4.0.0")
                (.setArtifactId (:name project))
                (.setName (:name project))
                (.setVersion (:version project))
                (.setGroupId (:group project)))]
    (doseq [dep (:dependencies project)]
      (.addDependency model (make-dependency dep)))
    (doseq [repo (concat (:repositories project) default-repos)]
      (.addRepository model (make-repository repo)))
    model))

(defn pom [project & [args]]
  ;; TODO: prompt if pom.xml exists
  (.writeModel (MavenProject. (make-model project))
               (writer (file (:root project) "pom.xml"))))
