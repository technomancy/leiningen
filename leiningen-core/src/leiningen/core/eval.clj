(ns leiningen.core.eval
  "Evaluate code inside the context of a project."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath])
  (:import (java.io PushbackReader)))

;; # Form Wrangling

(defn- injected-forms
  "Return the forms that need to be injected into the project for
  certain features (e.g. test selectors) to work."
  [project]
  ;; TODO: expose a way to disable these
  (with-open [rdr (-> "robert/hooke.clj" io/resource io/reader PushbackReader.)]
    `(do (ns ~'leiningen.util.injected)
         ~@(doall (take 6 (rest (repeatedly #(read rdr)))))
         (ns ~'user))))

(defn get-form-string
  "Serialize the given form and wrap it in other necessary mechanisms."
  [project form init]
  (let [form `(do ~init
                  ~(:project-init project)
                  ~@(let [user-clj (io/file (user/leiningen-home) "user.clj")]
                      (if (.exists user-clj)
                        [(list 'load-file (str user-clj))]))
                  ~(injected-forms project)
                  (set! ~'*warn-on-reflection*
                        ~(:warn-on-reflection project))
                  ~form)]
    ;; work around java's command line handling on windows
    ;; http://bit.ly/9c6biv This isn't perfect, but works for what's
    ;; currently being passed; see
    ;; http://www.perlmonks.org/?node_id=300286 for some of the
    ;; landmines involved in doing it properly
    (if (and (= (get-os) :windows) (not (:eval-in-leiningen project)))
      (pr-str (pr-str form))
      (pr-str form))))

;; TODO: this needs to be totally reworked; it doesn't fit well into
;; the whole leiningen-core separation.
(defn prep [{:keys [compile-path checksum-deps] :as project} skip-auto-compile]
  ;; (when (and (not (or ;; *skip-auto-compile*
  ;;                     skip-auto-compile)) compile-path
  ;;            (empty? (.list (io/file compile-path))))
  ;;   (binding [;; *silently* true
  ;;             ]
  ;;     (compile project)))
  ;; (when (or (empty? (find-deps-files project)) checksum-deps)
  ;;   (deps project))
  ;; This must exist before the project is launched.
  (.mkdirs (io/file compile-path))
  )

;; # Subprocess stuff

(defn- get-by-pattern
  "Gets a value from map m, but uses the keys as regex patterns, trying
  to match against k instead of doing an exact match."
  [m k]
  (m (first (drop-while #(nil? (re-find (re-pattern %) k))
                        (keys m)))))

(def ^{:private true} native-names
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

(defn native-arch-path
  "Path to the os/arch-specific directory containing native libs."
  [project]
  (when (and (get-os) (get-arch))
    (io/file (:native-path project) (name (get-os)) (name (get-arch)))))

(defn platform-nullsink []
  (io/file (if (= :windows (get-os))
          "NUL"
          "/dev/null")))

(defn- as-str [x]
  (if (instance? clojure.lang.Named x)
    (name x)
    (str x)))

(defn- d-property [[k v]]
  (format "-D%s=%s" (as-str k) v))

(defn- get-jvm-args
  "Calculate command-line arguments for launching java subprocess."
  [project]
  (let [native-arch-path (native-arch-path project)]
    `(~@(let [opts (System/getenv "JVM_OPTS")]
          (when (seq opts) [opts]))
      ~@(:jvm-opts project)
      ~@(:jvm-opts (user/settings))
      ~@(map d-property {:clojure.compile.path (:compile-path project)
                         (str (:name project) ".version") (:version project)
                         :clojure.debug (boolean (or (System/getenv "DEBUG")
                                                     (:debug project)))})
      ~@(when (and native-arch-path (.exists native-arch-path))
          [(d-property [:java-library-path native-arch-path])]))))

(defn- pump [reader out]
  (let [buffer (make-array Character/TYPE 1000)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (Thread/sleep 100)
        (recur (.read reader buffer))))))

(defn sh
  "A version of clojure.java.shell/sh that streams out/err."
  [& cmd]
  (let [proc (.exec (Runtime/getRuntime) (into-array cmd))]
    (with-open [out (io/reader (.getInputStream proc))
                err (io/reader (.getErrorStream proc))]
      (let [pump-out (doto (Thread. #(pump out *out*)) .start)
            pump-err (doto (Thread. #(pump err *err*)) .start)]
        (.join pump-out)
        (.join pump-err))
      (.waitFor proc))))

(defn eval-in-subprocess
  "Launch a subprocess for the given project to evaluate the given form."
  [project form-string]
  (apply sh `(~(or (System/getenv "JAVA_CMD") "java")
              "-cp" ~(string/join java.io.File/pathSeparatorChar
                                  (classpath/get-classpath project))
              ~@(get-jvm-args project)
              "clojure.main" "-e" ~form-string)))

(defn eval-in-leiningen
  "Support eval-in-project for plugins that run in Leiningen's own process."
  [project form-string]
  (when (:debug project)
    (System/setProperty "clojure.debug" "true"))
  ;; need to at least pretend to return an exit code
  (try (eval (read-string form-string))
       0
       (catch Exception e
         (.printStackTrace e)
         1)))

(defn eval-in-project
  "Executes form in an isolated classloader with the classpath and compile path
  set correctly for the project. If the form depends on any requires, put them
  in the init arg to avoid the Gilardi Scenario: http://technomancy.us/143"
  ([project form init]
     (prep project skip-auto-compile)
     ;; might only make sense to stringify the form in :subprocess eval
     (let [form-string (get-form-string project form init)]
       ;; TODO: support :eval-in :leiningen, :subprocess, or :classloader (default)
       ;; TODO: normalize old :eval-in-leiningen to new format in project/read
       (if (:eval-in-leiningen project)
         (eval-in-leiningen project form-string)
         (eval-in-subprocess project form-string))))
  ([project form] (eval-in-project project form nil)))
