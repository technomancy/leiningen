(ns leiningen.core.main
  (:require [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.utils :as utils]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.stacktrace :as stacktrace]
            [bultitude.core :as b]
            [cemerick.pomegranate.aether :as aether]))

(def aliases {"-h" "help", "-help" "help", "--help" "help", "-?" "help",
              "-v" "version", "-version" "version", "--version" "version",
              "überjar" "uberjar",
              "-o" ["with-profile" "+offline"]
              "-U" ["with-profile" "+update"]
              "cp" "classpath" "halp" "help"
              "with-profiles" "with-profile"
              "readme" ["help" "readme"]
              "tutorial" ["help" "tutorial"]
              "sample" ["help" "sample"]})

;; without a delay this loads profiles at the top-level which can
;; result in exceptions thrown outside of a nice catching context.
(def ^:private profile-aliases
  "User profile aliases, used only when Lein is not within a project."
  (delay (atom (-> (user/profiles) :user :aliases))))

(defn- get-and-dissoc!
  "Returns a value associated with a key in a hash map contained in an atom,
  removing it if it exists."
  [atom key]
  (when-let [[k v] (find @atom key)]
    (swap! atom dissoc key)
    v))

(defn- merge-alias-meta [task-vector task-name]
  (merge (select-keys (meta task-vector) [:pass-through-help])
         (meta task-name)))

(defn lookup-alias
  "Recursively look up aliases until the task is not an alias anymore. If
  task-name is a vector, calls lookup-alias on the first argument and returns a
  partially applied task. Discards already used aliases."
  [task-name project & [not-found]]
  (if (vector? task-name)
    (let [[t & args] task-name ;; never mind the poor naming here.
          resolved-task (lookup-alias t project not-found)
          task-vector (if (vector? resolved-task)
                        resolved-task
                        [resolved-task])
          merged-meta (merge-alias-meta task-vector task-name)]
      (-> task-vector
          (into args)
          (with-meta merged-meta)))
    (let [project-wo-alias (update-in project [:aliases] dissoc task-name)
          resolved-task (or (aliases task-name)
                            (get (:aliases project) task-name)
                            (when-not project
                              (get-and-dissoc! @profile-aliases task-name)))]
      (cond (nil? resolved-task) (or task-name not-found "help")
            (= task-name resolved-task) task-name
            :else (recur resolved-task project-wo-alias not-found)))))

(defn- lookup-task-var
  "Require and resolve a leiningen task from its name."
  [task-name]
  (or (utils/require-resolve (str "leiningen.plugin" task-name) task-name)
      (utils/require-resolve (str "leiningen." task-name) task-name)))

(declare remove-alias)

(defn- pass-through-help? [task-name project]
  (let [de-aliased (lookup-alias task-name project)]
    (if (vector? de-aliased)
      (or (:pass-through-help (meta de-aliased))
          (pass-through-help? (first de-aliased)
                              (remove-alias project task-name)))
      (:pass-through-help (meta (lookup-task-var de-aliased))))))

(defn task-args [[task-name & args] project]
  (let [pass-through? (pass-through-help? task-name project)]
    (if (and (= "help" (aliases (first args))) (not pass-through?))
      ["help" (cons task-name (rest args))]
      [(lookup-alias task-name project) (vec args)])))

(defn option-arg [str]
  (and str (cond (.startsWith str "--") (keyword str)
                 (.startsWith str ":") (keyword (subs str 1)))))

(defn parse-options
  "Given a sequence of strings, return a map of command-line-esque
  options with keyword-ized keys and a list of additional args:

  (parse-options [\"--chicken\"])
  => [{:--chicken true} []]

  (parse-options [\"--beef\" \"rare\"])
  => [{:--beef \"rare\"} []]

  (parse-options [\":fish\" \"salmon\"])
  => [{:fish \"salmon\"} []]

  (parse-options [\"salmon\" \"trout\"])
  => [{} [\"salmon\" \"trout\"]]

  (parse-options [\"--to-dir\" \"test2\" \"--ham\"])
  => [{:--ham true, :--to-dir \"test2\"} []]

  (parse-options [\"--to-dir\" \"test2\" \"--ham\" \"--\" \"pate\"])
  => [{:--ham true, :--to-dir \"test2\"} [\"pate\"]]"
  [options]
  (loop [m {}
         [first-arg second-arg & rest :as args] options]
    (if-let [option (and (not= "--" first-arg) (option-arg first-arg))]
      (if (or (not second-arg) (option-arg second-arg))
        (recur (assoc m option true) (if second-arg
                                       (cons second-arg rest)
                                       rest))
        (recur (assoc m option second-arg) rest))
      [m (if (= "--" first-arg)
           (if second-arg (cons second-arg rest) [])
           (or args []))])))

;; TODO for 3.0.0: debug, info and exit should be in a separate namespace
;; (io.clj?) to avoid cyclic deps.

(def ^:dynamic *debug* (System/getenv "DEBUG"))

(defn debug
  "Print if *debug* (from DEBUG environment variable) is truthy."
  [& args]
  (when *debug* (apply println args)))

(def ^:dynamic *info* (not (System/getenv "LEIN_SILENT")))

(defn info
  "Print if *info* (from LEIN_SILENT environment variable) is truthy."
  [& args]
  (when *info* (apply println args)))

(defn warn
  "Print to stderr if *info* is truthy."
  [& args]
  (when *info*
    (binding [*out* *err*]
      (apply println args))))

(def ^:dynamic *exit-process?*
  "Bind to false to suppress process termination." true)

(defn exit
  "Exit the process. Rebind *exit-process?* in order to suppress actual process
  exits for tools which may want to continue operating. Never call
  System/exit directly in Leiningen's own process."
  ([exit-code & msg]
     (if *exit-process?*
       (do (shutdown-agents)
           (System/exit exit-code))
       (throw (ex-info (if (seq msg)
                         (apply print-str msg)
                         "Suppressed exit")
                       {:exit-code exit-code :suppress-msg (empty? msg)}))))
  ([] (exit 0)))

(defn abort
  "Print msg to standard err and exit with a value of 1.
  Will not directly exit under some circumstances; see *exit-process?*."
  [& msg]
  (binding [*out* *err*]
    (when (seq msg)
      (apply println msg))
    (apply exit 1 msg)))

(defn- next-dist-row [s t x pprev prev]
  (let [t-len (count t)
        eq-chars (fn [x y] (= (nth s x) (nth t (dec y))))]
    (reduce (fn [row y]
              (let [min-step
                    (cond->
                     (min (inc (peek row)) ;; addition cost
                          (inc (get prev y)) ;; deletion cost
                          (cond-> (get prev (dec y)) ;; substitution cost
                                  (not (eq-chars x y)) inc))
                     (and (pos? x) (pos? (dec y)) ;; check for transposition
                          (eq-chars x (dec y))
                          (eq-chars (dec x) y)
                          (not (eq-chars x y)))
                     (min (inc (get pprev (- y 2)))))] ;; transposition cost
                (conj row min-step)))
            [(inc x)]
            (range 1 (inc t-len)))))

(defn- distance
  "Returns the Damerau–Levenshtein distance between two strings."
  [s t]
  (let [s-len (count s)
        t-len (count t)
        first-row (vec (range (inc t-len)))
        matrix (reduce (fn [matrix x]
                         (conj matrix
                               (next-dist-row s t x
                                              (peek (pop matrix))
                                              (peek matrix))))
                       [[] first-row]
                       (range s-len))]
    (peek (peek matrix))))

;; workaround for #2345; need to update this list if a new task is added
(def ^:private stock-tasks
  '[leiningen.change leiningen.check leiningen.classpath leiningen.clean
    leiningen.compile leiningen.deploy leiningen.deps leiningen.do
    leiningen.help leiningen.install leiningen.jar leiningen.javac leiningen.new
    leiningen.plugin leiningen.pom leiningen.release leiningen.repl
    leiningen.retest leiningen.run leiningen.search leiningen.show-profiles
    leiningen.test leiningen.trampoline leiningen.uberjar leiningen.update-in
    leiningen.upgrade leiningen.vcs leiningen.version leiningen.with-profile])

(defn tasks
  "Return a list of symbols naming all visible tasks."
  []
  (->> (b/namespaces-on-classpath :prefix "leiningen")
       (into stock-tasks)
       (filter #(re-find #"^leiningen\.(?!core|main|util)[^\.]+$" (name %)))
       (distinct)
       (sort)))

(defn suggestions
  "Suggest possible misspellings for task from list of tasks."
  [task tasks]
  (let [suggestions (into {} (for [t tasks
                                   :let [n (.replaceAll (name t)
                                                        "leiningen." "")]]
                               [n (distance n task)]))
        min (apply min (vals suggestions))]
    (if (<= min 3)
      (map first (filter #(= min (second %)) suggestions)))))

(defn ^:no-project-needed task-not-found [task & _]
  (binding [*out* *err*]
    (println (str "'" task "' is not a task. See 'lein help'."))
    (when-let [suggestions (suggestions task (tasks))]
      (println)
      (println "Did you mean this?")
      (doseq [suggestion suggestions]
        (println "        " suggestion))))
  (throw (ex-info "Task not found" {:exit-code 1 :suppress-msg true})))

(defn ^:no-project-needed function-not-found [task & _]
  (binding [*out* *err*]
    (println (str "leiningen." task
                  " is a Clojure namespace, but not a Leiningen task.")))
  (throw (ex-info "Task not found" {:exit-code 1 :suppress-msg true})))

(defn- drop-partial-args
  "Returns a function that returns a new list of arglist, where the
  args provided have been taken into consideration. All arglists
  should start with the project argument.

  A special case where the arglist is on the form [& ...] will be passed through
  without being transformed."
  [pargs]
  (let [argcount (count pargs)]
    (fn [arglists]
      (for [[project-arg & arglist] arglists
            :let [[fixed-args varargs] (split-with #(not= '& %) arglist)
                  new-fixed-args (drop argcount fixed-args)]]
        (if (= project-arg '&) ;; TODO: Clarify and remove this for 3.0.0
          (cons project-arg arglist)
          (cons project-arg (concat new-fixed-args varargs)))))))

(defn- splice-into-args
  "Alias vectors may include :project/key entries.
These get replaced with the corresponding values from the project map."
  [project args]
  (into [] (for [arg args]
             (if (and (keyword? arg) (= (namespace arg) "project"))
               (project (keyword (name arg)))
               arg))))

(defn- partial-task [task-var pargs]
  (with-meta
    (fn [project & args]
      (apply task-var project (splice-into-args project (concat pargs args))))
    (update-in (meta task-var) [:arglists] (drop-partial-args pargs))))

(defn resolve-task
  "Look up task function and perform partial application if applicable."
  ([task not-found]
     (let [[task & pargs] (if (coll? task) task [task])]
       (if-let [task-var (lookup-task-var task)]
         (partial-task task-var pargs)
         (not-found task))))
  ([task]
     (resolve-task task (if (find-ns (symbol (str "leiningen." task)))
                          #'function-not-found
                          #'task-not-found))))

(defn ^:internal matching-arity? [task args]
  (some (fn [parameters]
          (and (if (= '& (last (butlast parameters)))
                 (>= (count args) (- (count parameters) 3))
                 (= (count parameters) (inc (count args))))
               parameters))
        (:arglists (meta task))))

(defn- remove-alias
  "Removes an alias from the specified project and its metadata (which lies
   within :without-profiles) to avoid recursive alias calls."
  [project alias]
  (-> project
      (update-in [:aliases] #(if (map? %) (dissoc % alias) %))
      (vary-meta update-in [:without-profiles :aliases] dissoc alias)
      (vary-meta update-in [:profiles]
                 #(zipmap
                   (keys %)
                   (map (fn [p] (if (map? p) (remove-alias p alias) p))
                        (vals %))))))

(defn apply-task
  "Resolve task-name to a function and apply project and args if arity matches."
  [task-name project args]
  (let [[task-alias] (for [[k v] (:aliases project) :when (= v task-name)] k)
        project (and project (remove-alias project (or task-alias task-name)))
        task (resolve-task task-name)]
    (when-not (or (:root project) (:no-project-needed (meta task)))
      (abort "Couldn't find project.clj, which is needed for" task-name))
    (when-not (matching-arity? task args)
      (abort "Wrong number of arguments to" task-name "task."
             "\nExpected" (string/join " or " (map (comp vec next)
                                                   (:arglists
                                                    (meta task))))))
    (debug "Applying task" task-name "to" args)
    (apply task project args)))

(defn resolve-and-apply
  "Entry point for tasks run other tasks as if they were called from the CLI."
  [project args]
  (let [[task-name args] (task-args args project)]
    ;; See https://github.com/technomancy/leiningen/issues/2530
    ;; We can't assume the project uses the same clojure version as lein does.
    (binding [*print-namespace-maps* false]
      (apply-task task-name project args))))

(defn leiningen-version []
  (or (System/getenv "LEIN_VERSION")
      (with-open [reader (-> "META-INF/maven/leiningen/leiningen/pom.properties"
                             io/resource
                             io/reader)]
        (-> (doto (java.util.Properties.)
              (.load reader))
            (.getProperty "version")))))

(def ^:private exact-version-error
  "This project has :exact-lein-version set to \"%s\", while you have %s.")

(defn versions-match? [v1 v2]
  (let [v1 (string/trim (first (string/split v1 #"-" 2)))
        v2 (string/trim (first (string/split v2 #"-" 2)))]
    (= v1 v2)))

(defn- verify-exact-version
  [{:keys [exact-lein-version]}]
  (when-not (versions-match? exact-lein-version (leiningen-version))
    (abort (format exact-version-error
                   exact-lein-version
                   (leiningen-version)))))

(defn version-satisfies? [v1 v2]
  (let [v1 (map #(Integer. %) (re-seq #"\d+" (first (string/split v1 #"-" 2))))
        v2 (map #(Integer. %) (re-seq #"\d+" (first (string/split v2 #"-" 2))))]
    (loop [versions (map vector v1 v2)
           [seg1 seg2] (first versions)]
      (cond (empty? versions) true
            (= seg1 seg2) (recur (rest versions) (first (rest versions)))
            (> seg1 seg2) true
            (< seg1 seg2) false))))

;; packagers should replace this string!
(def ^:private min-version-warning
  "*** Warning: This project requires Leiningen %s, but you have %s ***

Get the latest version of Leiningen at https://leiningen.org or by executing
\"lein upgrade\".")

(defn- verify-min-version
  [{:keys [min-lein-version]}]
  (when-not (version-satisfies? (leiningen-version) min-lein-version)
    (info (format min-version-warning min-lein-version (leiningen-version)))))

(defn user-agent []
  (format "Leiningen/%s (Java %s; %s %s; %s)"
          (leiningen-version) (System/getProperty "java.vm.name")
          (System/getProperty "os.name") (System/getProperty "os.version")
          (System/getProperty "os.arch")))

(defn- configure-http
  "Set Java system properties controlling HTTP request behavior."
  []
  (System/setProperty "aether.connector.userAgent" (user-agent))
  (when-let [{:keys [host port non-proxy-hosts]} (classpath/get-proxy-settings)]
    (System/setProperty "http.proxyHost" host)
    (System/setProperty "http.proxyPort" (str port))
    (when non-proxy-hosts
      (System/setProperty "http.nonProxyHosts" non-proxy-hosts)))
  (when-let [{:keys [host port]} (classpath/get-proxy-settings "https_proxy")]
    (System/setProperty "https.proxyHost" host)
    (System/setProperty "https.proxyPort" (str port))))

(def ^:dynamic *cwd* (System/getProperty "user.dir"))

(defn default-project
  "Return the default project used when not in a project directory."
  []
  (-> (project/make {:eval-in :leiningen :prep-tasks []
                     :source-paths ^:replace []
                     :resource-paths ^:replace []
                     :test-paths ^:replace []})
      (project/init-project)))

(defn- insecure-http-abort [& _]
  (let [repo (promise)]
    (reify org.apache.maven.wagon.Wagon
      (getRepository [this])
      (setTimeout [this _])
      (setInteractive [this _])
      (addTransferListener [this _])
      (^void connect [this
                      ^org.apache.maven.wagon.repository.Repository the-repo
                      ^org.apache.maven.wagon.authentication.AuthenticationInfo _
                      ^org.apache.maven.wagon.proxy.ProxyInfoProvider _]
       (deliver repo the-repo) nil)
      (get [this resource _]
        (abort "Tried to use insecure HTTP repository without TLS:\n"
               (str (.getId @repo) ": " (.getUrl @repo) "\n " resource) "\n"
               "\nThis is almost certainly a mistake; for details see"
               "\nhttps://codeberg.org/leiningen/leiningen/src/main/doc/FAQ.md")))))

(defn -main
  "Command-line entry point."
  [& raw-args]
  (try
    (project/ensure-dynamic-classloader)
    (aether/register-wagon-factory! "http" insecure-http-abort)
    (user/init)
    (binding [project/*memoize-middleware* true]
      (let [project (if (.exists (io/file *cwd* "project.clj"))
                      (project/read (str (io/file *cwd* "project.clj")))
                      (default-project))]
        (when (:exact-lein-version project) (verify-exact-version project))
        (when (:min-lein-version project) (verify-min-version project))
        (configure-http)
        (resolve-and-apply project raw-args)))
    (catch Exception e
      (if (or *debug* (not (:exit-code (ex-data e))))
        (stacktrace/print-cause-trace e)
        (when-not (:suppress-msg (ex-data e))
          (println (.getMessage e))))
      (flush)
      (exit (:exit-code (ex-data e) 1))))
  (exit 0))
