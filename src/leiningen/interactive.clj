(ns leiningen.interactive
  "Enter interactive task shell."
  (:require [clojure.string :as string])
  (:use [leiningen.core :only [apply-task exit *interactive?*]]
        [leiningen.test :only [*exit-after-tests*]]
        [leiningen.repl :only [repl-server repl-socket-on
                               copy-out-loop poll-repl-connection]]
        [leiningen.compile :only [eval-in-project]]))

(def welcome "Welcome to Leiningen. Type help for a list of commands.")

(def prompt "lein> ")

(defn not-found [& _]
  (println "That's not a task. Use help to list all tasks."))

(defn- eval-client-loop [reader writer buffer socket]
  (let [len (.read reader buffer)
        output (String. buffer)]
    (when-not (neg? len)
      (.write *out* buffer 0 len)
      (flush)
      (when-not (.isClosed socket)
        (Thread/sleep 100)
        (recur reader writer buffer socket)))))

(defn eval-in-repl [connect project form & [_ _ init]]
  (let [[reader writer socket] (connect)]
    (.write writer (str "(do " (pr-str init)
                        (pr-str form) "\n" '
                        (.close *in*) ")\n"))
    (.flush writer)
    (try (eval-client-loop reader writer
                           (make-array Character/TYPE 1000) socket)
         0
         (catch Exception e
           (.printStackTrace e) 1)
         (finally
          (.close reader)
          (.close writer)))))

(defn print-prompt []
  (print prompt)
  (flush))

(defn task-repl [project]
  (print-prompt)
  (loop [input (.readLine *in*)]
    (when (and input (not= input "exit"))
      (let [[task-name & args] (string/split input #"\s")]
        ;; TODO: don't start a second repl server for repl task
        (try (apply-task task-name project args not-found)
             ;; TODO: not sure why, but repl seems to put an extra EOF on *in*
             (when (= "repl" task-name)
               (.read *in*))
             (catch Exception e
               (println (.getMessage e))))
        (print-prompt)
        (recur (.readLine *in*))))))

(defn interactive
  "Enter an interactive task shell."
  [project]
  (let [[port host] (repl-socket-on project)]
    (println welcome)
    (future
      (binding [*interactive?* true]
        (eval-in-project project `(do ~(repl-server project host port
                                                    :prompt '(constantly ""))
                                      ;; can't stop return value from printing
                                      (symbol "")))))
    (let [connect #(poll-repl-connection port 0 vector)]
      (binding [eval-in-project (partial eval-in-repl connect)
                *exit-after-tests* false
                *interactive?* true
                exit (fn exit [& _] (prn))]
        (task-repl project)))
    (exit)))
