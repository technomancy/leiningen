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

(defn- server-forms [project port ack-port start-msg?]
  [`(let [server# (clojure.tools.nrepl.server/start-server
                   :bind ~(repl-host project) :port ~port
                   :ack-port ~ack-port
                   :handler ~(handler-for project))
          port# (-> server# deref :ss .getLocalPort)]
      (when ~start-msg? (println "nREPL server started on port" port#))
      (spit ~(str (io/file (:target-path project) "repl-port")) port#)
      (.deleteOnExit (io/file ~(:target-path project) "repl-port"))
      @(promise))
   ;; TODO: remove in favour of :injections in the :repl profile
   `(do ~(when-let [init-ns (init-ns project)]
           `(try (require '~init-ns)
                 (catch Exception e# (println e#) (ns ~init-ns))))
        ~@(for [n (init-requires project)]
            `(try (require ~n)
                  (catch Throwable t#
                    (println "Error loading" (str ~n ":")
                             (or (.getMessage t#) (type t#))))))
        ~(-> project :repl-options :init))])

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

(def trampoline-profile {:dependencies '[^:displace
                                         [reply "0.1.10"
                                          :exclusions [org.clojure/clojure
                                                       ring/ring-core]]]})

(defn- trampoline-repl [project port]
  (let [options (dissoc (options-for-reply project :port port) :input-stream)
        profile (:leiningen/trampoline-repl (:profiles project)
                                            trampoline-profile)]
    (eval/eval-in-project
     (project/merge-profiles project [profile])
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

(defn server [project port headless?]
  (nrepl.ack/reset-ack-port!)
  (let [prep-blocker @eval/prep-blocker
        ack-port (-> @lein-repl-server deref :ss .getLocalPort)
        [start-form init-form] (server-forms project port ack-port headless?)]
    (-> (bound-fn []
          (binding [eval/*pump-in* false]
            (eval/eval-in-project project start-form init-form)))
        (Thread.) (.start))
    (when project @prep-blocker)
    (when headless? @(promise))
    (if-let [repl-port (nrepl.ack/wait-for-ack (-> project
                                                   :repl-options
                                                   (:timeout 60000)))]
      (do (println "nREPL server started on port" repl-port) repl-port)
      (main/abort "REPL server launch timed out."))))

(defn client [project attach]
  (when (and (string? attach) (.startsWith attach "http"))
    (require 'cemerick.drawbridge.client))
  (reply/launch-nrepl (options-for-reply project :attach attach)))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

Subcommands:

:start [:port port] (default) This will launch an nREPL server and
  connect a client to it. If a :port key is specified on the command
  line or present in the :repl-options map in project.clj, that port
  will be used for the server, otherwise it is chosen randomly.  When
  run outside of a project, it will run internally to Leiningen.

:headless [:port port]
  This will launch an nREPL server and wait, rather than connecting
  a client to it.

:connect [host:]port
  Connects to the nREPL server running at the given host (defaults to
  localhost) and port."
  ([project] (repl project ":start"))
  ([project subcommand & opts]
     (let [profiles [(:repl (:profiles project)) (:repl (user/profiles))]
           project (-> (project/merge-profiles project profiles)
                       (update-in [:eval-in] #(or % :leiningen)))
           port (or (opt-port opts) (repl-port project))]
       (case subcommand
         ":start" (if trampoline/*trampoline?*
                    (trampoline-repl project port)
                    (let [port (server project port false)]
                      (client project port)))
         ":headless" (let [[start init] (server-forms project port nil true)]
                       (eval/eval-in-project project start init))
         ":connect" (client project (or (first opts) port))
         (main/abort "Unknown subcommand")))))
