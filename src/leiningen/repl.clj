(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require (clojure set
                     main
                     [string :as s])
            [clojure.java.io :as io]
            (clojure.tools.nrepl [ack :as nrepl.ack]
                                 [server :as nrepl.server])
            (leiningen.core [eval :as eval]
                            [main :as main]
                            [user :as user]
                            [project :as project]
                            [classpath :as classpath])
            [leiningen.trampoline :as trampoline]
            [reply.main :as reply]))

(defn opt-port [opts]
  (when-let [port (second (drop-while #(not= % ":port") opts))]
    (Integer/valueOf port)))

(defn repl-port [project]
  (Integer/valueOf (or (user/getenv "LEIN_REPL_PORT")
                       (-> project :repl-options :port)
                       (-> (user/profiles) :user :repl-options :port)
                       0)))

(defn repl-host [project]
  (or (user/getenv "LEIN_REPL_HOST")
      (-> project :repl-options :host)
      (-> (user/profiles) :user :repl-options :host)
      "127.0.0.1"))

(defn connect-string [project opts]
  (as-> (str (first opts)) x
        (s/split x #":")
        (remove s/blank? x)
        (-> (drop-last (count x) [(repl-host project) (repl-port project)])
            (concat x))
        (s/join ":" x)))

(defn options-for-reply [project & {:keys [attach port]}]
  (as-> (:repl-options project) x
        (merge {:history-file (->> (if-let [root (:root project)]
                                     [root ".lein-repl-history"]
                                     [(user/leiningen-home) "repl-history"])
                                   (apply io/file)
                                   str)
                :input-stream System/in}
               x)
        (apply dissoc x (concat [:init] (when attach [:host :port])))
        (merge x (cond attach {:attach (str attach)}
                       port {:port port}
                       :else {}))
        (clojure.set/rename-keys x {:prompt :custom-prompt})
        (if (:port x) (update-in x [:port] str) x)))

(defn init-ns [{{:keys [init-ns]} :repl-options, :keys [main]}]
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

(defn- server-forms [project cfg ack-port start-msg?]
  [`(let [server# (clojure.tools.nrepl.server/start-server
                   :bind ~(:host cfg) :port ~(:port cfg)
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

(def trampoline-profile
  {:dependencies
   '[^:displace [reply "0.1.10"
                 :exclusions [org.clojure/clojure ring/ring-core]]]})

(defn- trampoline-repl [project port]
  (let [options (-> (options-for-reply project :port port)
                    (dissoc :input-stream))
        profile (:leiningen/trampoline-repl (:profiles project)
                                            trampoline-profile)]
    (eval/eval-in-project
     (project/merge-profiles project [profile])
     (if (:standalone options)
       `(reply.main/launch-standalone ~options)
       `(reply.main/launch-nrepl ~options))
     `(do (try (require '~(init-ns project)) (catch Throwable t#))
          (require ~@(init-requires project 'reply.main))))))

(def lein-repl-server
  (delay (nrepl.server/start-server
          :host (repl-host nil)
          :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn server [project cfg headless?]
  (nrepl.ack/reset-ack-port!)
  (let [prep-blocker @eval/prep-blocker
        ack-port (-> @lein-repl-server deref :ss .getLocalPort)]
    (-> (bound-fn []
          (binding [eval/*pump-in* false]
            (apply eval/eval-in-project project
                   (server-forms project cfg ack-port headless?))))
        (Thread.) (.start))
    (when project @prep-blocker)
    (when headless? @(promise))
    (if-let [repl-port (nrepl.ack/wait-for-ack
                        (-> project :repl-options (:timeout 60000)))]
      (do (println "nREPL server started on port" repl-port) repl-port)
      (main/abort "REPL server launch timed out."))))

(defn client [project attach]
  (when (and (string? attach) (.startsWith attach "http:"))
    (require 'cemerick.drawbridge.client))
  (reply/launch-nrepl (options-for-reply project :attach attach)))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

Subcommands:

<none> -> :start

:start [:port port] This will launch an nREPL server and connect a
  client to it. If the :port key is specified, or present in the
  :repl-options map in project.clj, that port will be used for the
  server, otherwise it is chosen randomly.  When starting outside of a
  project, the nREPL server will run internally to Leiningen.

:headless [:port port]
  This will launch an nREPL server and wait, rather than connecting
  a client to it.

:connect [dest]
  Connects to an already running nREPL server. Dest can be:
  - an HTTP URL -- connects to an HTTP nREPL endpoint;
  - host:port -- connects to the specified host and port;
  - port -- resolves host from the LEIN_REPL_HOST environment
      variable or :repl-options, in that order, and defaults to
      localhost.
  If no dest is given, resolves the port from :repl-options and the host
  as described above."
  ([project] (repl project ":start"))
  ([project subcommand & opts]
     (let [profiles [(:repl (:profiles project)) (:repl (user/profiles))]
           project (-> (project/merge-profiles project profiles)
                       (update-in [:eval-in] #(or % :leiningen)))]
       (if (= subcommand ":connect")
         (client project (doto (connect-string project opts)
                           (->> (println "Connecting to nREPL at"))))
         (let [cfg {:host (repl-host project)
                    :port (or (opt-port opts) (repl-port project))}]
           (case subcommand
             ":start" (if trampoline/*trampoline?*
                        (trampoline-repl project (:port cfg))
                        (->> (server project cfg false) (client project)))
             ":headless" (apply eval/eval-in-project project
                                (server-forms project cfg nil true))
             (main/abort "Unknown subcommand")))))))
