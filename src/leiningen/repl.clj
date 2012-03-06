(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require clojure.main
            [reply.main :as reply]
            [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [clojure.tools.nrepl.ack :as nrepl.ack]
            [clojure.tools.nrepl.server :as nrepl.server]
            [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath])
  (:use [leiningen.core.main :only [abort]]))

(def profile {:dependencies '[[org.clojure/tools.nrepl "0.2.0-beta1"
                               :exclusions [org.clojure/clojure]]
                              [clojure-complete "0.2.1"
                               :exclusions [org.clojure/clojure]]
                              [org.thnetos/cd-client "0.3.3"
                               :exclusions [org.clojure/clojure]]]})

(defn- start-server [project port ack-port]
  (if project
    (eval/eval-in-project (project/merge-profile project profile)
                          `(do (clojure.tools.nrepl.server/start-server
                                 :port ~port :ack-port ~ack-port))
                          '(do (require 'clojure.tools.nrepl.server)
                               (require 'complete.core)))
    (nrepl.server/start-server :port port :ack-port ack-port)))

(def lein-repl-server
  (delay (nrepl.server/start-server
           :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn- repl-port [project]
  (Integer. (or (System/getenv "LEIN_REPL_PORT")
                (:repl-port project)
                0)))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

USAGE: lein repl
This will launch an nREPL server behind the scenes that reply will connect to.
If a :repl-port key is present in project.clj, that port will be used for the
server, otherwise it is chosen randomly. If you run this command inside of a
project, it will be run in the context of that classpath. If the command is
run outside of a project, it'll be standalone and the classpath will be
that of Leiningen.

USAGE: lein repl :headless
This will launch an nREPL server and wait, rather than connecting reply to it."
  ([] (repl nil))
  ([project]
   (nrepl.ack/reset-ack-port!)
   (.start
     (Thread.
       (bound-fn []
         (start-server project
                       (repl-port project)
                       (-> @lein-repl-server deref :ss .getLocalPort)))))
   (if-let [repl-port (nrepl.ack/wait-for-ack (:repl-timeout project 30000))]
     (reply/launch-nrepl
       (merge
         {:attach (str repl-port)}
         (:reply-options project)))
     (println "REPL server launch timed out.")))
  ([project flag]
   (case flag
     ":headless" (let [ack-port (when-let [p (System/getenv "LEIN_REPL_ACK_PORT")]
                                  (Integer. p))]
                   (start-server project
                                 (repl-port project)
                                 ack-port)
                   (while true
                     (Thread/sleep Long/MAX_VALUE)))
     (abort "Unrecognized flag:" flag))))
