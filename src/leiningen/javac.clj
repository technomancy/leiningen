(ns leiningen.javac
  "Compile Java source files."
  (:use [leiningen.classpath :only [get-classpath-string]]
        [leiningen.util.paths :only [normalize-path]])
  (:require [lancet.core :as lancet])
  (:import (java.io File)))

(def ^{:doc "Default options for the java compiler."} *default-javac-options*
  {:debug "false" :fork "true"
   :includejavaruntime "yes"
   :includeantruntime "false"
   :source "1.5" :target "1.5"})

(defn- extract-javac-task
  "Extract a compile task from the given spec."
  [project [path & options]]
  (merge *default-javac-options*
         (:javac-options project)
         {:destdir (:compile-path project)
          :srcdir (normalize-path (:root project) path)
          :classpath (get-classpath-string project)}
         (apply hash-map options)))

(defn- extract-javac-tasks
  "Extract all compile tasks of the project."
  [project]
  (let [specs (:java-source-path project)]
    (for [spec (if (string? specs) [[specs]] specs)]
      (extract-javac-task project spec))))

(defn- run-javac-task
  "Compile the given task spec."
  [task-spec]
  (lancet/mkdir {:dir (:destdir task-spec)})
  (lancet/javac task-spec))

(defn javac
  "Compile Java source files.

Add a :java-source-path key to project.clj to specify where to find them."
  [project & [directory]]
  (doseq [task (extract-javac-tasks project)
          :when (or (nil? directory) (= directory (:srcdir task)))]
    (run-javac-task task)))

