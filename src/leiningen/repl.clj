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

(def profile {:dependencies '[^:displace
                               [org.clojure/tools.nrepl "0.2.0-beta9"
                                :exclusions [org.clojure/clojure]]
                              ^:displace
                               [clojure-complete "0.2.1"
                                :exclusions [org.clojure/clojure]]]})

(def reply-profile {:dependencies '[^:displace
                                     [org.thnetos/cd-client "0.3.4"
                                      :exclusions [org.clojure/clojure]]]})

(def trampoline-profile {:dependencies '[^:displace
                                          [reply "0.1.0-beta11"
                                           :exclusions [org.clojure/clojure]]]})

(defn- handler-for [{{:keys [nrepl-middleware nrepl-handler]} :repl-options}]
  (when (and nrepl-middleware nrepl-handler)
    (main/abort "Can only use one of" :nrepl-handler "or" :nrepl-middleware))
  (if nrepl-middleware
    `(clojure.tools.nrepl.server/default-handler
       ~@(map #(if (symbol? %) (list 'var %) %) nrepl-middleware))
    (or nrepl-handler '(clojure.tools.nrepl.server/default-handler))))

(defn- init-requires [{{:keys [nrepl-middleware nrepl-handler]} :repl-options
                       :as project}
                      & nses]
  (let [defaults '[clojure.tools.nrepl.server complete.core]
        nrepl-syms (->> (cons nrepl-handler nrepl-middleware)
                     (filter symbol?)
                     (map namespace)
                     (remove nil?)
                     (map symbol))]
    (for [n (concat defaults nrepl-syms nses)]
      (list 'quote n))))

(defn- start-server [project host port ack-port & [headless?]]
  (let [server-starting-form
        `(let [server# (clojure.tools.nrepl.server/start-server
                        :bind ~host :port ~port :ack-port ~ack-port
                        :handler ~(handler-for project))
               port# (-> server# deref :ss .getLocalPort)]
           (println "nREPL server started on port" port#)
           (spit ~(str (io/file (:target-path project) "repl-port")) port#)
           (.deleteOnExit (io/file ~(:target-path project) "repl-port"))
           @(promise))]
    (if project
      (eval/eval-in-project
       (project/merge-profiles project [(:repl (user/profiles) profile)
                                        (if-not headless? reply-profile)])
       server-starting-form
       `(require ~@(init-requires project)))
      (eval server-starting-form))))

(defn- repl-port [project]
  (Integer. (or (System/getenv "LEIN_REPL_PORT")
                (-> project :repl-options :port)
                0)))

(defn- repl-host [project]
  (or (System/getenv "LEIN_REPL_HOST")
      (-> project :repl-options :host)
      "127.0.0.1"))

(def lein-repl-server
  (delay (nrepl.server/start-server
          :host (repl-host nil)
          :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn- ack-port [project]
  (if-let [p (or (System/getenv "LEIN_REPL_ACK_PORT")
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

(defn- trampoline-repl [project]
  (let [options (options-for-reply project :port (repl-port project))
        profiles [(:repl (user/profiles) profile) trampoline-profile]]
    (eval/eval-in-project
     (project/merge-profiles project profiles)
     `(reply.main/launch-nrepl ~options)
     `(require ~@(init-requires project 'reply.main)))))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

USAGE: lein repl
This will launch an nREPL server behind the scenes
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
    (trampoline-repl project)
    (let [prep-blocker @eval/prep-blocker]
      (nrepl.ack/reset-ack-port!)
      (.start
       (Thread.
        (bound-fn []
          (start-server project (repl-host project) (repl-port project)
                        (-> @lein-repl-server deref :ss .getLocalPort)))))
      (when project @prep-blocker)
      (if-let [repl-port (nrepl.ack/wait-for-ack (-> project
                                                     :repl-options
                                                     (:timeout 30000)))]
        (reply/launch-nrepl (options-for-reply project :attach repl-port))
        (println "REPL server launch timed out.")))))
  ([project flag & opts]
   (case flag
     ":headless" (start-server project
                               (repl-host project) (repl-port project)
                               (ack-port project) :headless)
     ":connect" (do (require 'cemerick.drawbridge.client)
                    (reply/launch-nrepl {:attach (first opts)}))
     (main/abort "Unrecognized flag:" flag))))
