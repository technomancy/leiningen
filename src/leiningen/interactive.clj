(ns leiningen.interactive
  (:require [clojure.string :as string])
  (:use [leiningen.core :only [apply-task]]))

(defn not-found [& _]
  (println "That's not a task. Use \"lein help\" to list all tasks."))

(defn interactive
  "Enter an interactive shell for calling tasks without relaunching new JVMs."
  [project]
  (println "Welcome to Leiningen. Type \"help\" for a list of commands.")
  (loop []
    (flush)
    (print "lein> ")
    (flush)
    ;; TODO: integrate with tab-completion in jLine
    (let [input (.readLine *in*)]
      (when input
        (let [[task-name & args] (string/split input #"\s")]
          (apply-task task-name project args not-found)
          (recur))))))
