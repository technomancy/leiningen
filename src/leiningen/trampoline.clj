(ns leiningen.trampoline
  (:refer-clojure :exclude [trampoline])
  (:use [leiningen.main :only [apply-task task-not-found abort]])
  (:require [clojure.string :as string]
            [leiningen.core.eval :as eval]))

(def ^{:dynamic true} *trampoline?* false)

(defn win-batch? []
  (.endsWith (System/getProperty "leiningen.trampoline-file") ".bat"))

(defn write-trampoline [command]
  (spit (System/getProperty "leiningen.trampoline-file")
        (string/join " " (if (win-batch?)
                           command
                           (conj (vec (butlast command))
                                 (with-out-str (prn (last command))))))))

(defn trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates what needs to run in the project's process for the provided
task and runs it after Leiningen's own JVM process has exited rather
than as a subprocess of Leiningen's project.

Use this to save memory or to work around things like stdin issues.
Not compatible with chaining.

ALPHA: subject to change without warning."
  [project task-name & args]
  (let [command (atom nil)]
    (when (:eval-in-leiningen project)
      (println "Warning: trampoline has no effect with :eval-in-leiningen."))
    #_(binding [*trampoline?* true
              eval/sh (fn [& c] (reset! command c) 0)]
      (apply-task task-name project args task-not-found))
    (if @command
      (write-trampoline @command)
      (abort task-name "did not run any project code for trampolining."))))
