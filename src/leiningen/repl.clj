(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require [clojure.set]
            [clojure.main]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [nrepl.ack :as nrepl.ack]
            [nrepl.server :as nrepl.server]
            [cemerick.pomegranate :as pomegranate]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]
            [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [leiningen.trampoline :as trampoline]))

(defn- repl-port-file-vector
  "Returns the repl port file for this project as a vector."
  [project]
  (if-let [root (:root project)]
    [root ".nrepl-port"]
    [(user/leiningen-home) "repl-port"]))

(defn- repl-port-file-path
  "Returns the repl port file path for this project."
  [project]
  (.getPath (apply io/file (repl-port-file-vector project))))

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
        (catch Exception _))
      port)))

(defn ensure-port [s]
  (if (re-find #":\d+($|/.*$)" s)
    s
    (main/abort "Port is required. See `lein help repl`")))

(defn is-uri? [s]
  (boolean (and (string? s) (re-find #"^https?://" s))))

(defn string-from-file [arg]
  (if-let [filename-tmp (and (seq arg) (= "@" (subs arg 0 1)) (seq (subs arg 1)))]
    (let [filename (apply str filename-tmp)
          errmsg (str "The file '" filename "' can't be read.")]
      (if-let [content (try (slurp filename)
                            (catch Exception e
                            (main/abort errmsg)))]
        (s/trim content)
        (main/abort errmsg)))
      false))

(defn connect-string [project opts]
  (let [opt (str (first opts))]
    (if-let [sx (string-from-file opt)]
      (connect-string project [sx])
      (if (is-uri? opt)
        opt
        (as-> (s/split opt #":") x
              (remove s/blank? x)
              (-> (drop-last (count x) [(repl-host project) (client-repl-port project)])
                  (concat x))
              (s/join ":" x)
              (ensure-port x))))))

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
        (apply dissoc opts :init (if attach [:host :port]))
        (merge opts (cond attach {:attach (str attach)}
                          port {:port port}
                          :else {}))
        (clojure.set/rename-keys opts {:prompt :custom-prompt
                                       :welcome :custom-help})
        (if (:port opts) (update-in opts [:port] str) opts)))

(defn init-ns [{{:keys [init-ns]} :repl-options, :keys [main]}]
  (or init-ns (symbol (namespace main))))

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
         (nrepl.middleware/set-descriptor!
          {:requires #{(var nrepl.middleware.session/session)}
           :expects #{"eval"}})
         (alter-var-root (constantly @wrap-init-ns#))))))

(defn- handler-for [{{:keys [nrepl-middleware nrepl-handler]} :repl-options,
                     :as project}]
  (when (and nrepl-middleware nrepl-handler)
    (main/abort "Can only use one of" :nrepl-handler "or" :nrepl-middleware))
  (let [nrepl-middleware (remove nil? (concat [(wrap-init-ns project)]
                                              nrepl-middleware))]
    (or nrepl-handler
        `(nrepl.server/default-handler
           ~@(map #(if (symbol? %) (list 'var %) %) nrepl-middleware)))))

(defn- init-requires [{{:keys [nrepl-middleware nrepl-handler caught]}
                       :repl-options :as project} & nses]
  (let [defaults '[nrepl.server complete.core]
        nrepl-syms (->> (cons nrepl-handler nrepl-middleware)
                        (filter symbol?)
                        (map namespace)
                        (remove nil?)
                        (map symbol))
        caught (and caught (namespace caught) [(symbol (namespace caught))])]
    (for [n (concat defaults nrepl-syms nses caught)]
      (list 'quote n))))

(defn- ignore-sigint-form []
  `(when (try (Class/forName "sun.misc.Signal")
              (catch ClassNotFoundException e#))
     (try
       (sun.misc.Signal/handle
         (sun.misc.Signal. "INT")
         (proxy [sun.misc.SignalHandler] [] (handle [signal#])))
       (catch Throwable e#))))

(defn- server-forms [project cfg ack-port start-msg?]
  [`(let [server# (nrepl.server/start-server
                   :bind ~(:host cfg) :port ~(:port cfg)
                   :ack-port ~ack-port
                   :handler ~(handler-for project))
          port# (:port server#)
          repl-port-file# (apply io/file ~(repl-port-file-vector project))
          ;; TODO 3.0: remove legacy repl port support.
          legacy-repl-port# (if (.exists (io/file ~(:target-path project "")))
                              (io/file ~(:target-path project) "repl-port"))]
      (when ~start-msg?
        (println "nREPL server started on port" port# "on host" ~(:host cfg)
                 (str "- nrepl://" ~(:host cfg) ":" port#)))
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

(def reply-profile
  {:dependencies
   '[^:displace [reply "0.4.3" :exclusions [org.clojure/clojure ring/ring-core]]
     [clojure-complete "0.2.5"]]})

(defn- trampoline-repl [project port]
  (let [init-option (get-in project [:repl-options :init])
        init-code `(do
                     ~(if-let [ns# (init-ns project)] `(in-ns '~ns#))
                     ~init-option)
        options (-> (options-for-reply project :port port)
                    (assoc :custom-eval init-code)
                    (dissoc :input-stream))
        profile (:leiningen/trampoline-repl (:profiles project)
                                            reply-profile)]
    (eval/eval-in-project
     (project/merge-profiles project [profile])
     `(do (reply.main/launch '~options) (System/exit 0))
     `(do (try (require '~(init-ns project)) (catch Exception t#))
          (require ~@(init-requires project 'reply.main))))))

(def ack-server
  "The server which handles ack replies."
  (delay (nrepl.server/start-server
          :bind (repl-host nil)
          :handler (nrepl.ack/handle-ack nrepl.server/unknown-op))))

(defn nrepl-dependency? [{:keys [dependencies]}]
  (some (fn [[d]] (re-find #"nrepl" (str d))) dependencies))

;; NB: This function cannot happen in parallel (or be recursive) because of race
;; conditions in nrepl.ack.
(defn server [project cfg headless?]
  (nrepl.ack/reset-ack-port!)
  (when-not (nrepl-dependency? project)
    (main/info "Warning: no nREPL dependency detected.")
    (main/info "Be sure to include nrepl/nrepl in :dependencies"
               "of your profile."))
  (let [prep-blocker @eval/prep-blocker
        ack-port (:port @ack-server)]
    (-> (bound-fn []
          (binding [eval/*pump-in* false]
            (let [[evals requires]
                  (server-forms project cfg ack-port headless?)]
              (eval/eval-in-project project
                                    `(do ~(ignore-sigint-form) ~evals)
                                    requires))))
        (Thread.) (.start))
    (when project @prep-blocker)
    (when headless? @(promise))
    (if-let [repl-port (nrepl.ack/wait-for-ack
                        (get-in project [:repl-options :timeout] 60000))]
      (do (main/info "nREPL server started on port"
                     repl-port "on host" (:host cfg)
                     (str "- nrepl://" (:host cfg) ":" repl-port))
          repl-port)
      (main/abort "REPL server launch timed out."))))

(defn client [project attach]
  (when (is-uri? attach)
    (require 'drawbridge.client))
  (pomegranate/add-dependencies :coordinates (:dependencies reply-profile)
                                :repositories (map classpath/add-repo-auth
                                                   (:repositories project)))
  (let [launch (utils/require-resolve 'reply.main/launch-nrepl)]
    (launch (options-for-reply project :attach attach)))  )

(defn ^:no-project-needed repl
  "Start a repl session either with the current project or standalone.

Subcommands:

<none> -> :start

:start [:host host] [:port port]
  This will launch an nREPL server and connect a client to it.
  If the :host key is given, LEIN_REPL_HOST is set, or :host is present
  under :repl-options, that host will be attached to, defaulting to
  localhost otherwise, which will block remote connections. If the :port
  key is given, LEIN_REPL_PORT is set, or :port is present under
  :repl-options in the project map, that port will be used for
  the server, otherwise it is chosen randomly. When starting outside
  of a project, the nREPL server will run internally to Leiningen. When
  run under trampoline, the client/server step is skipped entirely; use
  the :headless command to start a trampolined server.

:headless [:host host] [:port port]
  This will launch an nREPL server and wait, rather than connecting
  a client to it.

:connect [dest]
  Connects to an already running nREPL server. Dest can be:
  - host:port -- connects to the specified host and port;
  - port -- resolves host from the LEIN_REPL_HOST environment
      variable or :repl-options, in that order, and defaults to
      localhost.
  If no dest is given, resolves the host resolved as described above
  and the port from LEIN_REPL_PORT, :repl-options, or .nrepl-port in
  the project root, in that order. Providing an argument that begins
  with @ and points to a filename containing a connect string will read
  that file and use its contents, allowing sensitive credentials to be
  kept out of the process table and shell history.

For connecting to HTTPS repl servers add [com.cemerick/drawbridge \"0.0.7\"]
to your :plugins list.

Note: the :repl profile is implicitly activated for this task. It cannot be
deactivated, but it can be overridden."

  ([project] (repl project ":start"))
  ([project subcommand & opts]
   (let [repl-profiles (project/profiles-with-matching-meta project :repl)
         project (project/merge-profiles project repl-profiles)]
     (if (= subcommand ":connect")
       (client project (doto (connect-string project opts)
                         (->> (main/info "Connecting to nREPL at"))))
       (let [cfg {:host (or (opt-host opts) (repl-host project))
                  :port (or (opt-port opts) (repl-port project))}]
         (utils/with-write-permissions (repl-port-file-path project)
           (case subcommand
             ":start" (if trampoline/*trampoline?*
                        (trampoline-repl project (:port cfg))
                        (->> (server project cfg false) (client project)))
             ":headless" (apply eval/eval-in-project project
                                (server-forms project cfg (ack-port project)
                                              true))
             (main/abort (str "Unknown subcommand " subcommand)))))))))

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
