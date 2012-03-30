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
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]))

(def profile {:dependencies '[[org.clojure/tools.nrepl "0.2.0-beta1"
                               :exclusions [org.clojure/clojure]]
                              [clojure-complete "0.2.1"
                               :exclusions [org.clojure/clojure]]
                              [org.thnetos/cd-client "0.3.3"
                               :exclusions [org.clojure/clojure]]]})

(defn- start-server [project port ack-port]
  (let [server-starting-form
          `(let [server# (clojure.tools.nrepl.server/start-server
                           :port ~port :ack-port ~ack-port)]
            (println "nREPL server started on port"
              (-> server# deref :ss .getLocalPort)))]
    (if project
      (eval/eval-in-project
        (project/merge-profile project profile)
        server-starting-form
        '(do (require 'clojure.tools.nrepl.server)
             (require 'complete.core)))
      (eval server-starting-form))))

(def lein-repl-server
  (delay (nrepl.server/start-server
           :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn- repl-port [project]
  (Integer. (or (System/getenv "LEIN_REPL_PORT")
                (:repl-port project)
                0)))

(defn- ack-port [project]
  (when-let [p (or (System/getenv "LEIN_REPL_ACK_PORT")
                   (:repl-ack-port project))]
    (Integer. p)))

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
This will launch an nREPL server and wait, rather than connecting reply to it.

USAGE: lein repl :connect [host:]port
Connects to the nREPL server running at the given host (defaults to localhost)
and port."
  ([] (repl nil))
  ([project]
     (nrepl.ack/reset-ack-port!)
     (let [prepped (promise)]
       (.start
        (Thread.
         (bound-fn []
           (start-server (vary-meta project assoc :prepped prepped)
                         (repl-port project)
                         (-> @lein-repl-server deref :ss .getLocalPort)))))
       @prepped
       (if-let [repl-port (nrepl.ack/wait-for-ack (:repl-timeout project 30000))]
         (reply/launch-nrepl (merge {:attach (str repl-port)}
                                    (:reply-options project)))
         (println "REPL server launch timed out."))))
  ([project flag & opts]
   (case flag
     ":headless" (do (start-server project
                       (repl-port project)
                       (ack-port project))
                     (while true
                       (Thread/sleep Long/MAX_VALUE)))
     ":connect" (reply/launch-nrepl {:attach (first opts)})
     (main/abort "Unrecognized flag:" flag))))
