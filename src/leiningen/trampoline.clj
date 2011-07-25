(ns leiningen.trampoline
  (:refer-clojure :exclude [trampoline])
  (:use [leiningen.core :only [apply-task task-not-found abort]]
        [leiningen.classpath :only [get-classpath-string]])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [leiningen.util.paths :as paths]))

(def *trampoline?* false)

(defn write-trampoline [command]
  (spit (System/getProperty "leiningen.trampoline-file") command))

(defn trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates what needs to run in the project's process for the provided
task and runs it after Leiningen's own JVM process has exited rather
than as a subprocess of Leiningen's project.

Use this to save memory or to work around things like Ant's stdin
issues. Not compatible with chaining.

ALPHA: subject to change without warning."
  [project task-name & args]
  (let [command (atom nil)]
    (when (:eval-in-leiningen project)
      (println "Warning: trampoline has no effect with :eval-in-leiningen."))
    (binding [*trampoline?* true
              shell/sh (fn [& c] (reset! command c) 0)]
      (apply-task task-name project args task-not-found))
    (if @command
      (write-trampoline (string/join " " @command))
      (abort task-name "is not trampolineable."))))
