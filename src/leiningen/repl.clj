(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require [clojure.set]
            [clojure.main]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.tools.nrepl.ack :as nrepl.ack]
            [clojure.tools.nrepl.server :as nrepl.server]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [leiningen.trampoline :as trampoline]
            [reply.main :as reply]))

(defn lookup-opt [opt-key opts]
  (second (drop-while #(not= % opt-key) opts)))

(defn opt-host [opts]
  (lookup-opt ":host" opts))

(defn opt-port [opts]
  (if-let [port (lookup-opt ":port" opts)]
    (Integer/valueOf port)))

(defn ack-port [project]
  (if-let [p (or (user/getenv "LEIN_REPL_ACK_PORT")
                 (-> project :repl-options :ack-port))]
    (Integer/valueOf p)))

(defn repl-port [project]
  (Integer/valueOf (or (user/getenv "LEIN_REPL_PORT")
                       (-> project :repl-options :port)
                       0)))

(defn repl-host [project]
  (or (user/getenv "LEIN_REPL_HOST")
      (-> project :repl-options :host)
      "127.0.0.1"))

(defn client-repl-port [project]
  (let [port (repl-port project)]
    (if (= port 0)
      (try
        (slurp (io/file (:root project) ".nrepl-port"))
        (catch Exception _ (main/abort "Port is required")))
      port)))

(defn connect-string [project opts]
  (as-> (str (first opts)) x
        (s/split x #":")
        (remove s/blank? x)
        (-> (drop-last (count x) [(repl-host project) (client-repl-port project)])
            (concat x))
        (s/join ":" x)))

(defn options-for-reply [project & {:keys [attach port]}]
  (as-> (:repl-options project) opts
        (merge {:history-file (->> (if-let [root (:root project)]
                                     [root ".lein-repl-history"]
                                     [(user/leiningen-home) "repl-history"])
                                   (apply io/file)
                                   str)
                :input-stream System/in
                ;; TODO: once reply/#114 is fixed; add (user/help) back in and
                ;; move other source/javadoc/etc references into longer help.
                :welcome (list 'println (slurp (io/resource "repl-welcome")))}
               opts)
        (apply dissoc opts (concat [:init] (if attach [:host :port])))
        (merge opts (cond attach {:attach (str attach)}
                          port {:port port}
                          :else {}))
        (clojure.set/rename-keys opts {:prompt :custom-prompt
                                       :welcome :custom-help})
        (if (:port opts) (update-in opts [:port] str) opts)))

(defn init-ns [{{:keys [init-ns]} :repl-options, :keys [main]}]
  (or init-ns main))

(defn- wrap-init-ns [project]
  (if-let [init-ns (init-ns project)]
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
          port# (:port server#)
          repl-port-file# (apply io/file ~(if (:root project)
                                            [(:root project) ".nrepl-port"]
                                            [(user/leiningen-home) "repl-port"]))
          legacy-repl-port# (if (.exists (io/file ~(:target-path project)))
                              (io/file ~(:target-path project) "repl-port"))]
      (when ~start-msg?
        (println "nREPL server started on port" port# "on host" ~(:host cfg)))
      (spit (doto repl-port-file# .deleteOnExit) port#)
      (when legacy-repl-port#
        (spit (doto legacy-repl-port# .deleteOnExit) port#))
      @(promise))
   ;; TODO: remove in favour of :injections in the :repl profile
   `(do ~(when-let [init-ns (init-ns project)]
           `(try (doto '~init-ns require in-ns)
                 (catch Exception e# (println e#) (ns ~init-ns))))
        ~@(for [n (init-requires project)]
            `(try (require ~n)
                  (catch Throwable t#
                    (println "Error loading" (str ~n ":")
                             (or (.getMessage t#) (type t#))))))
        ~(-> project :repl-options :init))])

(def trampoline-profile
  {:dependencies
   '[^:displace [reply "0.2.1"
                 :exclusions [org.clojure/clojure ring/ring-core]]]})

(defn- trampoline-repl [project port]
  (let [init-option (get-in project [:repl-options :init])
        init-code `(do
                     ~(if-let [ns# (init-ns project)] `(in-ns '~ns#))
                     ~init-option)
        options (-> (options-for-reply project :port port)
                    (assoc :custom-eval init-code)
                    (dissoc :input-stream))
        profile (:leiningen/trampoline-repl (:profiles project)
                                            trampoline-profile)]
    (eval/eval-in-project
     (project/merge-profiles project [profile])
     `(do (reply.main/launch '~options) nil)
     `(do (try (require '~(init-ns project)) (catch Throwable t#))
          (require ~@(init-requires project 'reply.main))))))

(def ack-server
  "The server which handles ack replies."
  (delay (nrepl.server/start-server
          :bind (repl-host nil)
          :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn nrepl-dependency? [{:keys [dependencies]}]
  (some (fn [[d]] (re-find #"tools.nrepl" (str d))) dependencies))

;; NB: This function cannot happen in parallel (or be recursive) because of race
;; conditions in nrepl.ack.
(defn server [project cfg headless?]
  (nrepl.ack/reset-ack-port!)
  (when-not (nrepl-dependency? project)
    (main/info "Warning: no nREPL dependency detected.")
    (main/info "Be sure to include org.clojure/tools.nrepl in :dependencies"
               "of your profile."))
  (let [prep-blocker @eval/prep-blocker
        ack-port (:port @ack-server)]
    (-> (bound-fn []
          (binding [eval/*pump-in* false]
            (apply eval/eval-in-project project
                   (server-forms project cfg ack-port headless?))))
        (Thread.) (.start))
    (when project @prep-blocker)
    (when headless? @(promise))
    (if-let [repl-port (nrepl.ack/wait-for-ack
                        (get-in project [:repl-options :timeout] 60000))]
      (do (main/info "nREPL server started on port"
                     repl-port "on host" (:host cfg))
          repl-port)
      (main/abort "REPL server launch timed out."))))

(defn client [project attach]
  (when (and (string? attach) (.startsWith attach "http:"))
    (require 'cemerick.drawbridge.client))
  (reply/launch-nrepl (options-for-reply project :attach attach)))

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

Subcommands:

<none> -> :start

:start [:host host] [:port port] This will launch an nREPL server
  and connect a client to it. If the :host key is given, or present
  under :repl-options, that host will be attached to, defaulting
  to localhost otherwise, which will block remote connections.
  If the :port key is given, or present under :repl-options in
  the project map, that port will be used for the server, otherwise
  it is chosen randomly. When starting outside of a project,
  the nREPL server will run internally to Leiningen.

:headless [:host host] [:port port]
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
     (let [project (project/merge-profiles project [:repl])]
       (if (= subcommand ":connect")
         (client project (doto (connect-string project opts)
                           (->> (main/info "Connecting to nREPL at"))))
         (let [cfg {:host (or (opt-host opts) (repl-host project))
                    :port (or (opt-port opts) (repl-port project))}]
           (case subcommand
             ":start" (if trampoline/*trampoline?*
                        (trampoline-repl project (:port cfg))
                        (->> (server project cfg false) (client project)))
             ":headless" (apply eval/eval-in-project project
                                (server-forms project cfg (ack-port project)
                                              true))
             (main/abort (str "Unknown subcommand " subcommand))))))))

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
