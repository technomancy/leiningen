(ns leiningen.core.eval
  "Evaluate code inside the context of a project."
  (:require [classlojure.core :as cl]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cemerick.pomegranate :as pomegranate]
            [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leiningen.core.classpath :as classpath])
  (:import (org.sonatype.aether.resolution DependencyResolutionException)))

;; # OS detection

(defn- get-by-pattern
  "Gets a value from map m, but uses the keys as regex patterns, trying
  to match against k instead of doing an exact match."
  [m k]
  (m (first (drop-while #(nil? (re-find (re-pattern %) k))
                        (keys m)))))

(def ^:private native-names
  {"Mac OS X" :macosx "Windows" :windows "Linux" :linux
   "FreeBSD" :freebsd "OpenBSD" :openbsd
   "amd64" :x86_64 "x86_64" :x86_64 "x86" :x86 "i386" :x86
   "arm" :arm "SunOS" :solaris "sparc" :sparc "Darwin" :macosx})

(defn get-os
  "Returns a keyword naming the host OS."
  []
  (get-by-pattern native-names (System/getProperty "os.name")))

(defn get-arch
  "Returns a keyword naming the host architecture"
  []
  (get-by-pattern native-names (System/getProperty "os.arch")))

(defn platform-nullsink []
  (io/file (if (= :windows (get-os))
          "NUL"
          "/dev/null")))

;; # Form Wrangling

(defn prep [project]
  ;; This must exist before the project is launched.
  (.mkdirs (io/file (:compile-path project "/tmp")))
  (try (classpath/get-classpath project)
       (catch DependencyResolutionException e
         (main/info (.getMessage e))
         (main/info "Check :dependencies and :repositories for typos.")
         (main/info "It's possible the specified jar is not in any repository.")
         (main/info "If so, see \"Free-floating Jars\" under http://j.mp/repeatability")
         (main/abort)))
  (doseq [task (:prep-tasks project)]
    (main/apply-task task (dissoc project :prep-tasks) []))
  (.mkdirs (io/file (:compile-path project "/tmp")))
  (when-let [prepped (:prepped (meta project))]
    (deliver prepped true)))

;; # Subprocess stuff

(defn native-arch-path
  "Path to the os/arch-specific directory containing native libs."
  [project]
  (when (and (get-os) (get-arch))
    (io/file (:native-path project) (name (get-os)) (name (get-arch)))))

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

(defn ^{:internal true} get-jvm-opts-from-env [env-opts]
  (when (seq env-opts)
    (reduce join-broken-arg [] (.split env-opts " "))))

(defn- get-jvm-args
  "Calculate command-line arguments for launching java subprocess."
  [project]
  (let [native-arch-path (native-arch-path project)]
    `(~@(get-jvm-opts-from-env (System/getenv "JVM_OPTS"))
      ~@(:jvm-opts project)
      ~@(map d-property {:clojure.compile.path (:compile-path project)
                         (str (:name project) ".version") (:version project)
                         :clojure.debug (boolean (or (System/getenv "DEBUG")
                                                     (:debug project)))})
      ~@(when (and native-arch-path (.exists native-arch-path))
          [(d-property [:java.library.path native-arch-path])])
      ~@(when-let [{:keys [host port]} (classpath/get-proxy-settings)]
          [(d-property [:http.proxyHost host])
           (d-property [:http.proxyPort port])]))))

(defn- pump [reader out]
  (let [buffer (make-array Character/TYPE 1000)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (Thread/sleep 100)
        (recur (.read reader buffer))))))

(def ^:dynamic *dir* (System/getProperty "user.dir"))

(def ^:dynamic *env* nil)

(defn sh
  "A version of clojure.java.shell/sh that streams out/err."
  [& cmd]
  (let [env (and *env* (into-array String (map name (apply concat *env*))))
        proc (.exec (Runtime/getRuntime) (into-array cmd) env (io/file *dir*))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (.destroy proc))))
    (with-open [out (io/reader (.getInputStream proc))
                err (io/reader (.getErrorStream proc))]
      (let [pump-out (doto (Thread. (bound-fn [] (pump out *out*))) .start)
            pump-err (doto (Thread. (bound-fn [] (pump err *err*))) .start)]
        (.join pump-out)
        (.join pump-err))
      (.waitFor proc))))

;; work around java's command line handling on windows
;; http://bit.ly/9c6biv This isn't perfect, but works for what's
;; currently being passed; see http://www.perlmonks.org/?node_id=300286
;; for some of the landmines involved in doing it properly
(defn- form-string [form]
  (if (= (get-os) :windows)
    (pr-str (pr-str form))
    (pr-str form)))

(defn- classpath-arg [project]
  (if (:bootclasspath project)
    [(apply str "-Xbootclasspath/a:"
            (interpose java.io.File/pathSeparatorChar
                       (classpath/get-classpath project)))]
    ["-cp" (string/join java.io.File/pathSeparatorChar
                        (classpath/get-classpath project))]))

(defn shell-command [project form]
  `(~(or (:java-cmd project) (System/getenv "JAVA_CMD") "java")
    ~@(classpath-arg project)
    ~@(get-jvm-args project)
    "clojure.main" "-e" ~(form-string form)))

(defmulti eval-in
  "Evaluate the given from in either a subprocess or the leiningen process."
  (fn [project _] (:eval-in project)) :default :subprocess)

(defmethod eval-in :subprocess [project form]
  (binding [*dir* (:root project)]
    (let [exit-code (apply sh (shell-command project form))]
      (when (pos? exit-code)
        (throw (Exception. (str "Process exited with " exit-code)))))))

(defmethod eval-in :trampoline [project form]
  (deliver (:trampoline-promise (meta project))
           (shell-command project form)))

(defmethod eval-in :classloader [project form]
  (let [classpath   (map io/file (classpath/get-classpath project))
        classloader (cl/classlojure classpath)]
    (doseq [opt (get-jvm-args project)
            :when (.startsWith opt "-D")
            :let [[_ k v] (re-find #"^-D(.*?)=(.*)$" opt)]]
      (System/setProperty k v))
    (cl/eval-in classloader form)))

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
  "Executes form in an isolated classloader with the classpath and compile path
  set correctly for the project. If the form depends on any requires, put them
  in the init arg to avoid the Gilardi Scenario: http://technomancy.us/143"
  ([project form init]
     (prep project)
     (eval-in project
              `(do ~init
                   ~@(:injections project)
                   (set! ~'*warn-on-reflection*
                         ~(:warn-on-reflection project))
                   ~form)))
  ([project form] (eval-in-project project form nil)))
