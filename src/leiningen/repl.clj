(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require clojure.main
            clojure.set
            [reply.main :as reply]
            [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [leiningen.trampoline :as trampoline]
            [clojure.tools.nrepl.ack :as nrepl.ack]
            [clojure.tools.nrepl.server :as nrepl.server]
            [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]))

(def profile {:dependencies '[[org.clojure/tools.nrepl "0.2.0-beta6"
                               :exclusions [org.clojure/clojure]]
                              [clojure-complete "0.2.1"
                               :exclusions [org.clojure/clojure]]
                              [org.thnetos/cd-client "0.3.4"
                               :exclusions [org.clojure/clojure]]]})

(def trampoline-profile {:dependencies '[[reply "0.1.0-beta7"
                                         :exclusions [org.clojure/clojure]]]})

(defn- start-server [project port ack-port]
  (let [server-starting-form
          `(let [server# (clojure.tools.nrepl.server/start-server
                           :port ~port :ack-port ~ack-port)]
            (println "nREPL server started on port"
              (-> server# deref :ss .getLocalPort))
            (while true (Thread/sleep Long/MAX_VALUE)))]
    (if project
      (eval/eval-in-project
       (project/merge-profiles project [(:repl (user/profiles) profile)])
        server-starting-form
        '(do (require 'clojure.tools.nrepl.server)
             (require 'complete.core)))
      (eval server-starting-form))))

(def lein-repl-server
  (delay (nrepl.server/start-server
           :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn- repl-port [project]
  (Integer. (or (System/getenv "LEIN_REPL_PORT")
                (-> project :repl-options :port)
                0)))

(defn- repl-host [project]
  (or (System/getenv "LEIN_REPL_PORT")
      (-> project :repl-options :host)))

(defn- ack-port [project]
  (when-let [p (or (System/getenv "LEIN_REPL_ACK_PORT")
                   (-> project :repl-options :ack-port))]
    (Integer. p)))

(defn options-for-reply [project & {:keys [attach port]}]
  (let [repl-options (:repl-options project)]
    (clojure.set/rename-keys
      (merge
        repl-options
        {:init (if-let [init-ns (or (:init-ns repl-options) (:main project))]
                 `(do (require '~init-ns) (in-ns '~init-ns)
                      ~(:init repl-options))
                 (:init repl-options))}
        (cond
          attach
            {:attach (if-let [host (repl-host project)]
                       (str host ":" attach)
                       (str attach))}
          port
            {:port (str port)}
          :else
            {}))
      {:prompt :custom-prompt
       :init :custom-init})))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

USAGE: lein repl This will launch an nREPL server behind the scenes
that reply will connect to. If a :port key is present in
the :repl-options map in project.clj, that port will be used for the
server, otherwise it is chosen randomly. If you run this command
inside of a project, it will be run in the context of that classpath.
If the command is run outside of a project, it'll be standalone and
the classpath will be that of Leiningen.

USAGE: lein repl :headless
This will launch an nREPL server and wait, rather than connecting reply to it.

USAGE: lein repl :connect [host:]port
Connects to the nREPL server running at the given host (defaults to localhost)
and port."
  ([] (repl nil))
  ([project]
  (if trampoline/*trampoline?*
    (let [options (options-for-reply project :port (repl-port project))
          profiles [(:repl (user/profiles) profile) trampoline-profile]]
      (eval/eval-in-project
       (project/merge-profiles project profiles)
        `(reply.main/launch-nrepl ~options)
        '(do (require 'reply.main)
             (require 'clojure.tools.nrepl.server)
             (require 'complete.core))))
    (do
     (nrepl.ack/reset-ack-port!)
     (let [prepped (promise)]
       (.start
        (Thread.
         (bound-fn []
           (start-server (and project (vary-meta project assoc
                                                 :prepped prepped))
                         (repl-port project)
                         (-> @lein-repl-server deref :ss .getLocalPort)))))
       (and project @prepped)
       (if-let [repl-port (nrepl.ack/wait-for-ack (or (-> project
                                                          :repl-options
                                                          :timeout)
                                                      30000))]
         (reply/launch-nrepl (options-for-reply project :attach repl-port))
         (println "REPL server launch timed out."))))))
  ([project flag & opts]
   (case flag
     ":headless" (do (start-server project
                       (repl-port project)
                       (ack-port project)))
     ":connect" (reply/launch-nrepl {:attach (first opts)})
     (main/abort "Unrecognized flag:" flag))))
