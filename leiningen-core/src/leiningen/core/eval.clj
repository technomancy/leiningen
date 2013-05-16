(ns leiningen.core.eval
  "Evaluate code inside the context of a project."
  (:require [classlojure.core :as cl]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cemerick.pomegranate :as pomegranate]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.utils :as utils])
  (:import (com.hypirion.io Pipe ClosingPipe)))

(def ^:private arch-options
  {:x86 ["-d32"] :x86_64 ["-d64"]})

(def ^:deprecated get-os
  "Returns a keyword naming the host OS. Deprecated, use
  leiningen.core.utils/get-os instead."
  utils/get-os)

(def ^:deprecated get-arch
  "Returns a keyword naming the host architecture. Deprecated, use
leiningen.core.utils/get-arch instead."
  utils/get-arch)

(def ^:deprecated platform-nullsink
  "Returns a file destination that will discard output.  Deprecated, use
leiningen.core.utils/platform-nullsink instead."
  utils/platform-nullsink)

;; # Preparing for eval-in-project

(defn run-prep-tasks
  "Execute all the prep-tasks. A task can either be a string, or a
  vector if it takes arguments. see :prep-tasks in sample.project.clj
  for examples"
  [{:keys [prep-tasks] :as project}]
  (doseq [task prep-tasks]
    (let [[task-name & task-args] (if (vector? task) task [task])
          task-name (main/lookup-alias task-name project)]
      (main/apply-task task-name (dissoc project :prep-tasks) task-args))))

;; Some tasks
(defonce ^{:doc "Block on this to wait till the project is fully prepped."}
  prep-blocker (atom (promise)))

(defn prep
  "Before we can run eval-in-project we need to prep the project by running
  javac, compile, and any other tasks the project specifies."
  [project]
  ;; This must exist before the project is launched.
  (when (:root project)
    (.mkdirs (io/file (:compile-path project "/tmp"))))
  (classpath/resolve-dependencies :dependencies project)
  (run-prep-tasks project)
  (deliver @prep-blocker true)
  (reset! prep-blocker (promise)))

;; # Subprocess stuff

(defn native-arch-paths
  "Paths to the os/arch-specific directory containing native libs."
  [project]
  (let [os (:os project (get-os))
        arch (:arch project (get-arch))
        native-path (:native-path project)]
    (if (and os arch)
      (conj
       (->> (:dependencies project)
            (map classpath/get-native-prefix)
            (remove nil?)
            (map #(io/file native-path %)))
       (io/file native-path (name os) (name arch))))))

(defn- as-str [x]
  (if (instance? clojure.lang.Named x)
    (name x)
    (str x)))

(defn- d-property [[k v]]
  (format "-D%s=%s" (as-str k) v))

;; TODO: this would still screw up with something like this:
;; export JAVA_OPTS="-Dmain.greeting=\"hello -main\" -Xmx512m"
(defn- join-broken-arg [args x]
  (if (= \- (first x))
    (conj args x)
    (conj (vec (butlast args))
          (str (last args) " " x))))

(defn ^:internal get-jvm-opts-from-env [env-opts]
  (and (seq env-opts)
       (reduce join-broken-arg []
               (.split (string/trim env-opts) " "))))

(defn- get-jvm-args
  "Calculate command-line arguments for launching java subprocess."
  [project]
  (let [native-arch-paths (native-arch-paths project)]
    `(~@(get-jvm-opts-from-env (System/getenv "JVM_OPTS"))
      ~@(:jvm-opts project)
      ~@(get arch-options (:arch project))
      ;; TODO: support -Xverify:none
      ~@(map d-property {:clojure.compile.path (:compile-path project)
                         (str (:name project) ".version") (:version project)
                         :file.encoding (or (System/getProperty "file.encoding") "UTF-8")
                         :clojure.debug (boolean (or (System/getenv "DEBUG")
                                                     (:debug project)))})
      ~@(if native-arch-paths
          (let [extant-paths (filter #(.exists %) native-arch-paths)]
            (if (seq extant-paths)
              [(d-property [:java.library.path
                            (string/join java.io.File/pathSeparatorChar
                                         extant-paths)])])))
      ~@(when-let [{:keys [host port non-proxy-hosts]} (classpath/get-proxy-settings)]
          [(d-property [:http.proxyHost host])
           (d-property [:http.proxyPort port])
           (d-property [:http.nonProxyHosts non-proxy-hosts])])
      ~@(when-let [{:keys [host port]} (classpath/get-proxy-settings "https_proxy")]
          [(d-property [:https.proxyHost host])
           (d-property [:https.proxyPort port])]))))

(def ^:dynamic *dir*
  "Directory in which to start subprocesses with eval-in-project or sh."
  (System/getProperty "user.dir"))

(def ^:dynamic *env*
  "Environment map with which to start subprocesses with eval-in-project or sh.
  Merged into the current environment unless ^:replace metadata is attached."
  nil)

(def ^:dynamic *pump-in*
  "Rebind this to false to disable forwarding *in* to subprocesses."
  true)

(def drip-env
  {"DRIP_INIT" nil
   "DRIP_INIT_CLASS" nil})

(defn- overridden-env
  "Returns an overridden version of the current environment as an Array of
  Strings of the form name=val, suitable for passing to Runtime#exec."
  [env]
  (->> (if (:replace (meta env))
         env
         (merge {} (System/getenv) drip-env env))
       (filter val)
       (map #(str (name (key %)) "=" (val %)))
       (into-array String)))

(defn sh
  "A version of clojure.java.shell/sh that streams in/out/err."
  [& cmd]
  (when *pump-in*
    (utils/rebind-io!))
  (let [env (overridden-env *env*)
        proc (.exec (Runtime/getRuntime) (into-array cmd) env (io/file *dir*))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (.destroy proc))))
    (with-open [out (io/reader (.getInputStream proc))
                err (io/reader (.getErrorStream proc))
                in (.getOutputStream proc)]
      (let [pump-out (doto (Pipe. out *out*) .start)
            pump-err (doto (Pipe. err *err*) .start)
            pump-in (ClosingPipe. System/in in)]
        (when *pump-in* (.start pump-in))
        (.join pump-out)
        (.join pump-err)
        (let [exit-value (.waitFor proc)]
          (when *pump-in*
            (.kill System/in)
            (.join pump-in)
            (.resurrect System/in))
          exit-value)))))

(defn- form-string [form eval-in]
  (if (and (= (get-os) :windows) (not= :trampoline eval-in))
    ;; On windows if a parameter is in double quotes, then all we need
    ;; to worry about are double quotes, which we must escape
    (string/replace (pr-str form) "\"" "\\\"")
    (pr-str form)))

(defn- agent-arg [coords file]
  (format "-javaagent:%s%s" file (if-let [o (:options (apply hash-map coords))]
                                   (str "=" o) "")))

(defn ^:internal classpath-arg [project]
  (let [classpath-string (string/join java.io.File/pathSeparatorChar
                                      (classpath/get-classpath project))
        agent-tree (classpath/get-dependencies :java-agents project)
        ;; Seems like you'd expect dependency-files to walk the whole tree
        ;; here, but it doesn't, which is what we want. but maybe a bug?
        agent-jars (aether/dependency-files (aether/dependency-hierarchy
                                             (:java-agents project) agent-tree))]
    `(~@(map agent-arg (:java-agents project) agent-jars)
      ~@(if (:bootclasspath project)
          [(str "-Xbootclasspath/a:" classpath-string)]
          ["-classpath" classpath-string]))))

(defn shell-command
  "Calculate vector of strings needed to evaluate form in a project subprocess."
  [project form]
  `(~(or (:java-cmd project) (System/getenv "JAVA_CMD") "java")
    ~@(classpath-arg project)
    ~@(get-jvm-args project)
    "clojure.main" "-e" ~(form-string form (:eval-in project))))

;; # eval-in multimethod

(defmulti eval-in
  "Evaluate the given form in various contexts."
  ;; Force it to be a keyword so that we can accept symbols too. That
  ;; way ^:replace and ^:displace metadata can be applied.
  (fn [project _] (keyword (name (:eval-in project :subprocess)))))

(defmethod eval-in :subprocess [project form]
  (binding [*dir* (:root project)]
    (let [exit-code (apply sh (shell-command project form))]
      (when (pos? exit-code)
        (throw (ex-info "Subprocess failed" {:exit-code exit-code}))))))

(defonce trampoline-forms (atom []))
(defonce trampoline-profiles (atom []))

(defmethod eval-in :trampoline [project form]
  (swap! trampoline-forms conj form)
  (swap! trampoline-profiles conj (select-keys project
                                               [:dependencies :source-paths
                                                :resource-paths :test-paths])))

(defmethod eval-in :classloader [project form]
  (when-let [classpath (map io/file (classpath/ext-classpath project))]
    (cl/wrap-ext-classloader classpath))
  (let [classpath   (map io/file (classpath/get-classpath project))
        classloader (cl/classlojure classpath)]
    (doseq [opt (get-jvm-args project)
            :when (.startsWith opt "-D")
            :let [[_ k v] (re-find #"^-D(.*?)=(.*)$" opt)]]
      (if (= k "java.library.path")
        (cl/alter-java-library-path!
         (constantly (string/split v (re-pattern java.io.File/pathSeparator))))
        (System/setProperty k v)))
    (try (cl/eval-in classloader form)
         (catch Exception e
           (println (str "Error evaluating in classloader: "
                         (class e) ":" (.getMessage e)))
           (.printStackTrace e)
           (throw (ex-info "Classloader eval failed" {:exit-code 1}))))))

(defmethod eval-in :nrepl [project form]
  (require 'clojure.tools.nrepl)
  (let [port-file (io/file (:target-path project) "repl-port")
        connect (resolve 'clojure.tools.nrepl/connect)
        client (resolve 'clojure.tools.nrepl/client)
        client-session (resolve 'clojure.tools.nrepl/client-session)
        message (resolve 'clojure.tools.nrepl/message)
        recv (resolve 'clojure.tools.nrepl.transport/recv)]
    (if (.exists port-file)
      (let [transport (connect :host "localhost"
                               :port (Integer. (slurp port-file)))
            client (client-session (client transport Long/MAX_VALUE))]
        (message client {:op "eval" :code (pr-str form)})
        (doseq [{:keys [out err status]} (repeatedly #(recv transport 100))
                :while (not (some #{"done" "interrupted" "error"} status))]
          (when out (println out))
          (when err (binding [*out* *err*] (println err)))))
      ;; TODO: warn that repl couldn't be used?
      (eval-in (assoc project :eval-in :subprocess) form))))

(defmethod eval-in :leiningen [project form]
  (when (:debug project)
    (System/setProperty "clojure.debug" "true"))
  ;; :dependencies are loaded the same way as plugins in eval-in-leiningen
  (project/load-plugins project :dependencies)
  (doseq [path (classpath/get-classpath project)]
    (pomegranate/add-classpath path))
  (doseq [opt (get-jvm-args project)
          :when (.startsWith opt "-D")
          :let [[_ k v] (re-find #"^-D(.*?)=(.*)$" opt)]]
    (System/setProperty k v))
  (eval form))

(defn eval-in-project
  "Executes form in isolation with the classpath and compile path set correctly
  for the project. If the form depends on any requires, put them in the init arg
  to avoid the Gilardi Scenario: http://technomancy.us/143"
  ([project form init]
     (prep project)
     (when (:warn-on-reflection project)
       (println "WARNING: :warn-on-reflection is deprecated; use :global-vars."))
     (eval-in project
              `(do (set! ~'*warn-on-reflection*
                         ~(:warn-on-reflection project))
                   ~@(map (fn [[k v]] `(set! ~k ~v)) (:global-vars project))
                   ~init
                   ~@(:injections project)
                   ~form)))
  ([project form] (eval-in-project project form nil)))
