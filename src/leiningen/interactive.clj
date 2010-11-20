(ns leiningen.interactive
  (:require [clojure.string :as string])
  (:use [leiningen.core :only [apply-task]]
        [leiningen.repl :only [repl-server repl-socket-on
                               copy-out-loop poll-repl-connection]]
        [leiningen.compile :only [eval-in-project]]))

(def welcome "Welcome to Leiningen. Type help for a list of commands.")

(defn not-found [& _]
  (println "That's not a task. Use help to list all tasks."))

(defn- eval-client-loop [reader writer buffer eof]
  (let [len (.read reader buffer)
        output (String. buffer)]
    (when-not (or (neg? len) (re-find (re-pattern eof) output))
      (.write *out* buffer 0 len)
      (flush)
      (Thread/sleep 100)
      (recur reader writer buffer eof))))

(defn eval-in-repl [connect project form & [args]]
  (let [[reader writer] (connect)
        eof (str (java.util.UUID/randomUUID))]
    (.write writer (format "%s\n:%s\n" (pr-str form) eof))
    (.flush writer)
    (try (eval-client-loop reader writer (make-array Character/TYPE 1000) eof)
         0
         (catch Exception e
           (println (.getMessage e))
           (.printStackTrace e)
           1)
         (finally
          (.close reader)
          (.close writer)))))

(defn task-repl [project]
  (flush)
  (print "lein> ")
  (flush)
  (let [input (.readLine *in*)]
    (when (and input (not= input "exit"))
      (let [[task-name & args] (string/split input #"\s")]
        (apply-task task-name project args not-found)
        (recur project)))))

(defn interactive
  "Enter an interactive shell for calling tasks without relaunching new JVMs."
  [project]
  (let [[port host] (repl-socket-on project)]
    (future
      (eval-in-project project `(do ~(repl-server project host port
                                                  :prompt '(constantly ""))
                                    ~welcome)))
    (let [connect #(poll-repl-connection port 0 vector)]
      (binding [eval-in-project (partial eval-in-repl connect)]
        (task-repl project)))))
