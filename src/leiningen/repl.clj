(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require [clojure.main]
            [clojure.set]
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

(def reply-profile {:dependencies '[^:displace
                                    [org.thnetos/cd-client "0.3.6"
                                     :exclusions [org.clojure/clojure]]]})

(def trampoline-profile {:dependencies '[^:displace
                                         [reply "0.1.10"
                                          :exclusions [org.clojure/clojure]]]})

(def base-profile {:dependencies '[^:displace
                                   [org.clojure/tools.nrepl "0.2.1"
                                    :exclusions [org.clojure/clojure]]
                                   ^:displace
                                   [clojure-complete "0.2.2"
                                    :exclusions [org.clojure/clojure]]]})

(defn profiles-for [project trampoline? reply?]
  [(if reply? (:leiningen/reply (:profiles project) reply-profile))
   (if trampoline? (:leiningen/trampoline-repl (:profiles project)
                                               trampoline-profile))
   (:leiningen/repl (:profiles project) base-profile)
   (:repl (:profiles project)) (:repl (user/profiles))])

(defn- init-ns [{{:keys [init-ns]} :repl-options, :keys [main]}]
  (or init-ns main))

(defn- wrap-init-ns [project]
  (when-let [init-ns (init-ns project)]
    ;; set-descriptor! currently nREPL only accepts a var
    `(with-local-vars
         [wrap-init-ns#
          (fn [h#]
            ;; this needs to be a var, since it's in the nREPL session
            (with-local-vars [init-ns-sentinel# nil]
              (fn [{:keys [~'session] :as msg#}]
                (when-not (@~'session init-ns-sentinel#)
                  (swap! ~'session assoc
                         (var *ns*)
                         (try (require '~init-ns) (create-ns '~init-ns)
                              (catch Throwable t# (create-ns '~'user)))
                         init-ns-sentinel# true))
                (h# msg#))))]
       (doto wrap-init-ns#
         (clojure.tools.nrepl.middleware/set-descriptor!
          {:requires #{(var clojure.tools.nrepl.middleware.session/session)}
           :expects #{"eval"}})
         (alter-var-root (constantly @wrap-init-ns#))))))

(defn- handler-for [{{:keys [nrepl-middleware nrepl-handler]} :repl-options,
                     :as project}]
  (when (and nrepl-middleware nrepl-handler)
    (main/abort "Can only use one of" :nrepl-handler "or" :nrepl-middleware))
  (let [nrepl-middleware (remove nil? (concat [(wrap-init-ns project)]
                                              nrepl-middleware))]
    (or nrepl-handler
        `(clojure.tools.nrepl.server/default-handler
           ~@(map #(if (symbol? %) (list 'var %) %) nrepl-middleware)))))

(defn- init-requires [{{:keys [nrepl-middleware nrepl-handler]} :repl-options
                       :as project} & nses]
  (let [defaults '[clojure.tools.nrepl.server complete.core]
        nrepl-syms (->> (cons nrepl-handler nrepl-middleware)
                        (filter symbol?)
                        (map namespace)
                        (remove nil?)
                        (map symbol))]
    (for [n (concat defaults nrepl-syms nses)]
      (list 'quote n))))

(defn- repl-port [project]
  (Integer. (or (System/getenv "LEIN_REPL_PORT")
                (-> project :repl-options :port)
                (-> (user/profiles) :user :repl-options :port)
                0)))

(defn- repl-host [project]
  (or (System/getenv "LEIN_REPL_HOST")
      (-> project :repl-options :host)
      "127.0.0.1"))

(defn- start-server [project ack-port headless?]
  (let [server-starting-form
        `(let [server# (clojure.tools.nrepl.server/start-server
                        :bind ~(repl-host project) :port ~(repl-port project)
                        :ack-port ~ack-port
                        :handler ~(handler-for project))
               port# (-> server# deref :ss .getLocalPort)]
           (when ~headless? (println "nREPL server started on port" port#))
           (spit ~(str (io/file (:target-path project) "repl-port")) port#)
           (.deleteOnExit (io/file ~(:target-path project) "repl-port"))
           @(promise))]
    (eval/eval-in-project
     (if (= :leiningen (:eval-in project))
       project
       (project/merge-profiles project (profiles-for project false
                                                     (not headless?))))
     `(do ~(-> project :repl-options :init)
          ~server-starting-form)
     ;; TODO: remove in favour of :injections in the :repl profile
     `(do ~(when-let [init-ns (init-ns project)]
             `(try (require '~init-ns)
                (catch Exception e# (println e#) (ns ~init-ns))))
          ~@(for [n (init-requires project)]
              `(try (require ~n)
                    (catch Throwable t#
                      (println "Error loading" (str ~n ":")
                               (or (.getMessage t#) (type t#))))))))))

(def lein-repl-server
  (delay (nrepl.server/start-server
          :host (repl-host nil)
          :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn- ack-port [project]
  (if-let [p (or (System/getenv "LEIN_REPL_ACK_PORT")
                 (-> project :repl-options :ack-port))]
    (Integer. p)))

(defn options-for-reply [project & {:keys [attach port]}]
  (let [history-file (if (:root project)
                       ;; we are in project
                       "./.lein-repl-history"
                       ;; outside of project
                       (str (io/file (user/leiningen-home)
                                     "repl-history")))
        repl-options (merge {:history-file history-file,
                             :input-stream System/in}
                            (:repl-options project))]
    (clojure.set/rename-keys
     (merge (dissoc repl-options :init)
            (cond attach {:attach (if-let [host (repl-host project)]
                                    (str host ":" attach) (str attach))}
                  port {:port (str port)}
                  :else {}))
     {:prompt :custom-prompt})))

(defn- trampoline-repl [project port]
  (let [options (dissoc (options-for-reply project :port port) :input-stream)]
    (eval/eval-in-project
     (project/merge-profiles project (profiles-for project :trampoline true))
     (if (:standalone options)
       `(reply.main/launch-standalone ~options)
       `(reply.main/launch-nrepl ~options))
     `(do (try (require '~(init-ns project)) (catch Throwable t#))
          (require ~@(init-requires project 'reply.main))))))

(defn- opt-port [opts]
  (when-let [port (first
                   (for [[i o] (map-indexed vector opts) :when (= o ":port")]
                     (try (nth opts (inc i))
                          (catch Exception _))))]
    (Integer. port)))

(defn server [project headless?]
  (let [prep-blocker @eval/prep-blocker]
    (nrepl.ack/reset-ack-port!)
    (-> (bound-fn []
          (binding [eval/*pump-in* false]
            (start-server project (-> @lein-repl-server deref :ss .getLocalPort)
                          headless?)))
        (Thread.) (.start))
    (when project @prep-blocker)
    (when headless? @(promise))
    (if-let [repl-port (nrepl.ack/wait-for-ack (-> project
                                                   :repl-options
                                                   (:timeout 60000)))]
      (do (println "nREPL server started on port" repl-port) repl-port)
      (main/abort "REPL server launch timed out."))))

(defn client [project host attach]
  (when (and (string? attach) (.startsWith attach "http"))
    (require 'cemerick.drawbridge.client))
  (reply/launch-nrepl (options-for-reply project :attach attach)))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

Subcommands:

:start (default)
  This will launch an nREPL server and connect a client to it. If
  a :port key is present in the :repl-options map in project.clj, that
  port will be used for the server, otherwise it is chosen randomly.
  When run outside of a project, it will run internally to Leiningen.

:headless [:port port]
  This will launch an nREPL server and wait, rather than connecting
  a client to it.

:connect [host:]port
  Connects to the nREPL server running at the given host (defaults to
  localhost) and port."
  ([project] (repl project ":start"))
  ([project subcommand & opts]
     (let [host (repl-host project)
           port (or (opt-port opts) (repl-port project))]
       (case subcommand
         ":start" (if trampoline/*trampoline?*
                    (trampoline-repl project port)
                    (let [port (server project false)]
                      (client project host port)))
         ":headless" (start-server project (ack-port project) true)
         ":connect" (client project host (or (first opts) port))
         (main/abort "Unknown subcommand")))))

;; A note on testing the repl task: it has a number of modes of operation
;; which need to be tested individually:
;; * :start (normal operation)
;; * :headless (server-only)
;; * :connect (client-only)

;; These three modes should really each be tested in each of these contexts:
;; * :eval-in :subprocess (default)
;; * :eval-in :trampoline
;; * :eval-in :leiningen (:connect prolly doesn't need separate testing here)

;; Visualizing a 3x3 graph with checkboxes is an exercise left to the reader.

;; Possibly worth testing in TERM=dumb (no completion) as well as a regular
;; terminal, but that doesn't need to happen separately for each
;; subcommand. This is more about testing reply than the repl task itself.
