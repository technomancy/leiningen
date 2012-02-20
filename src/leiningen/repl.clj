(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require clojure.main
            [reply.main :as reply]
            [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [clojure.tools.nrepl :as nrepl]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]))

(def profile {:dependencies '[[org.clojure/tools.nrepl "0.0.5"
                               :exclusions [org.clojure/clojure]]
                              [clojure-complete "0.1.4"
                               :exclusions [org.clojure/clojure]]
                              [org.thnetos/cd-client "0.3.3"
                               :exclusions [org.clojure/clojure]]]})

(defn- start-server [project port ack-port]
  (if project
    (eval/eval-in-project (project/merge-profile project profile)
                          `(clojure.tools.nrepl/start-server ~port ~ack-port)
                          '(do (require 'clojure.tools.nrepl)
                               (require 'complete)))
    (nrepl/start-server port ack-port)))

(def lein-repl-server (delay (nrepl/start-server)))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

This will launch an nREPL server behind the scenes that reply will connect to.
If a :repl-port key is present in project.clj, that port will be used for the
server, otherwise it is chosen randomly. If you run this command inside of a
project, it will be run in the context of that classpath. If the command is
run outside of a project, it'll be standalone and the classpath will be
that of Leiningen."
  ([] (repl nil))
  ([project]
   (nrepl/reset-ack-port!)
   (.start
     (Thread.
       (bound-fn []
         (start-server project
                       (Integer.
                         (or (System/getenv "LEIN_REPL_PORT")
                             (:repl-port project)
                             0))
                       (-> @lein-repl-server first .getLocalPort)))))
   (reply/launch-nrepl
     (merge
      {:attach (str (nrepl/wait-for-ack (:repl-timeout project 30000)))}
       (:reply-options project)))))
