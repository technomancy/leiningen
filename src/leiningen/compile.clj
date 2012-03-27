(ns leiningen.compile
  "Compile Clojure source into .class files."
  (:use [leiningen.deps :only [deps find-deps-files]]
        [leiningen.core :only [defdeprecated user-settings *interactive?*]]
        [leiningen.javac :only [javac]]
        [leiningen.classpath :only [get-classpath-string]]
        [clojure.java.io :only [file resource reader copy]]
        [leiningen.util.ns :only [namespaces-in-dir]])
  (:require [leiningen.util.paths :as paths]
            [clojure.string :as string])
  (:refer-clojure :exclude [compile])
  (:import (java.io PushbackReader)))

(declare compile)

(def ^{:dynamic true} *silently* false)

(def ^{:dynamic true} *skip-auto-compile* false)

(defn- regex?
  "Returns true if we have regex class"
  [str-or-re]
  (instance? java.util.regex.Pattern str-or-re))

(defn- matching-namespaces [re-or-sym namespaces]
  (if (regex? re-or-sym)
    (filter #(re-find re-or-sym (name %)) namespaces)
    [re-or-sym]))

(defn- find-namespaces-by-regex
  "Trying to generate list of namespaces, matching to given regexs"
  [project nses]
  (let [project-nses (namespaces-in-dir (:source-path project))]
    (mapcat #(matching-namespaces % project-nses) nses)))

(defn- compile-main? [{:keys [main source-path] :as project}]
  (and main (not (:skip-aot (meta main)))
       (.exists (file source-path (paths/ns->path main)))))

(defn compilable-namespaces
  "Returns a seq of the namespaces that are compilable, regardless of whether
  their class files are present and up-to-date."
  [project]
  (when (:namespaces project)
    (println "WARNING: :namespaces in project.clj is deprecated; use :aot."))
  (let [nses (or (:aot project) (:namespaces project))
        nses (if (= :all nses)
               (namespaces-in-dir (:source-path project))
               (find-namespaces-by-regex project nses))]
    (if (compile-main? project)
      (conj nses (:main project))
      nses)))

(defn stale-namespaces
  "Return a seq of namespaces that are both compilable and that have missing or
  out-of-date class files."
  [project]
  (filter
   (fn [n]
     (let [clj-path (paths/ns->path n)
           class-file (file (:compile-path project)
                            (.replace clj-path "\\.clj" "__init.class"))]
       (or (not (.exists class-file))
           (> (.lastModified (file (:source-path project) clj-path))
              (.lastModified class-file)))))
   (compilable-namespaces project)))

 ;; eval-in-project

(defdeprecated get-os paths/get-os)

(defdeprecated get-os paths/get-arch)

(defn platform-nullsink []
  (file (if (= :windows (paths/get-os))
          "NUL"
          "/dev/null")))

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

(defn ^{:internal true} get-jvm-args [project]
  (let [native-arch-path (paths/native-arch-path project)]
    `(~@(get-jvm-opts-from-env (System/getenv "JVM_OPTS"))
      ~@(:jvm-opts project)
      ~@(:jvm-opts (user-settings))
      ~@(map d-property {:clojure.compile.path (:compile-path project)
                         (str (:name project) ".version") (:version project)
                         :clojure.debug (boolean (or (System/getenv "DEBUG")
                                                     (:debug project)))})
      ~@(when (and native-arch-path (.exists native-arch-path))
          [(d-property [:java.library.path native-arch-path])]))))

(defn- injected-forms []
  (with-open [rdr (-> "robert/hooke.clj" resource reader PushbackReader.)]
    `(do (ns ~'leiningen.util.injected)
         ~@(doall (take 6 (rest (repeatedly #(read rdr)))))
         (ns ~'user))))

(defn get-readable-form [java project form init]
  (let [form `(do ~init
                  ~(:project-init project)
                  ~@(let [user-clj (file (paths/leiningen-home) "user.clj")]
                      (if (.exists user-clj)
                        [(list 'load-file (str user-clj))]))
                  ~(injected-forms)
                  (set! ~'*warn-on-reflection*
                        ~(:warn-on-reflection project))
                  (try ~form
                       ;; non-daemon threads will prevent process from exiting;
                       ;; see http://tinyurl.com/2ueqjkl
                       (finally
                        (when (and ~(:shutdown-agents project false)
                                   (not= "1.5" (System/getProperty
                                                "java.specification.version"))
                                   ~(not *interactive?*))
                          (shutdown-agents)))))]
    ;; work around java's command line handling on windows
    ;; http://bit.ly/9c6biv This isn't perfect, but works for what's
    ;; currently being passed; see
    ;; http://www.perlmonks.org/?node_id=300286 for some of the
    ;; landmines involved in doing it properly
    (if (and (= (paths/get-os) :windows) (not (:eval-in-leiningen project)))
      (pr-str (pr-str form))
      (pr-str form))))

(defn prep [{:keys [compile-path checksum-deps] :as project} skip-auto-compile]
  (when (and (not (or *skip-auto-compile* skip-auto-compile)) compile-path
             (empty? (.list (file compile-path))))
    (binding [*silently* true]
      (compile project)))
  (when (or (empty? (find-deps-files project)) checksum-deps)
    (deps project))
  (when compile-path
    (.mkdirs (file compile-path))))

(defn eval-in-leiningen [project form-string]
  ;; bootclasspath workaround: http://dev.clojure.org/jira/browse/CLJ-673
  (require '[clojure walk repl])
  (require '[clojure.java io shell browse])
  (when (:debug project)
    (System/setProperty "clojure.debug" "true"))
  (doseq [opt (get-jvm-args project)
          :when (.startsWith opt "-D")
          :let [[_ k v] (re-find #"^-D(.*?)=(.*)$" opt)]]
    (System/setProperty k v))
  ;; need to at least pretend to return an exit code
  (try (binding [*warn-on-reflection* (:warn-on-reflection project), *ns* *ns*]
         (eval (read-string form-string)))
       0
       (catch Exception e
         (.printStackTrace e)
         1)))

(defn- pump [reader out]
  (let [buffer (make-array Character/TYPE 1000)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (Thread/sleep 100)
        (recur (.read reader buffer))))))

;; clojure.java.shell/sh doesn't let you stream out/err
(defn sh [& cmd]
  (let [proc (.exec (Runtime/getRuntime) (into-array cmd))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (.destroy proc))))
    (with-open [out (reader (.getInputStream proc))
                err (reader (.getErrorStream proc))]
      (let [pump-out (doto (Thread. #(pump out *out*)) .start)
            pump-err (doto (Thread. #(pump err *err*)) .start)]
        (.join pump-out)
        (.join pump-err))
      (.waitFor proc))))

(defn eval-in-subprocess [project form-string]
  (apply sh `(~(or (System/getenv "JAVA_CMD") "java")
              "-cp" ~(get-classpath-string project)
              ~@(get-jvm-args project)
              "clojure.main" "-e" ~form-string)))

(defn eval-in-project
  "Executes form in an isolated classloader with the classpath and compile path
  set correctly for the project. If the form depends on any requires, put them
  in the init arg to avoid the Gilardi Scenario: http://technomancy.us/143"
  [project form & [handler skip-auto-compile init]]
  (when skip-auto-compile
    (println "WARNING: eval-in-project's skip-auto-compile arg is deprecated."))
  (prep project skip-auto-compile)
  (let [form-string (get-readable-form nil project form init)]
    (if (:eval-in-leiningen project)
      (eval-in-leiningen project form-string)
      (eval-in-subprocess project form-string))))

 ;; .class file cleanup

(defn- has-source-package?
  "Test if the class file's package exists as a directory in source-path."
  [project f source-path]
  (and source-path
       (let [[[parent] [_ _ proxy-mod-parent]]
             (->> f, (iterate #(.getParentFile %)),
                  (take-while identity), rest,
                  (split-with #(not (re-find #"^proxy\$" (.getName %)))))]
         (.isDirectory (file (.replace (.getPath (or proxy-mod-parent parent))
                                       (:compile-path project)
                                       source-path))))))

(defn- class-in-project? [project f]
  (or (has-source-package? project f (:source-path project))
      (has-source-package? project f (:java-source-path project))
      (.exists (file (str (.replace (.getParent f)
                                    (:compile-path project)
                                    (:source-path project)) ".clj")))))

(defn- relative-path [project f]
  (let [root-length (if (= \/ (last (:compile-path project)))
                      (count (:compile-path project))
                      (inc (count (:compile-path project))))]
    (subs (.getAbsolutePath f) root-length)))

(defn- blacklisted-class? [project f]
  ;; true indicates all non-project classes are blacklisted
  (or (true? (:clean-non-project-classes project))
      (some #(re-find % (relative-path project f))
            (:clean-non-project-classes project))))

(defn- whitelisted-class? [project f]
  (or (class-in-project? project f)
      (and (:class-file-whitelist project)
           (re-find (:class-file-whitelist project)
                    (relative-path project f)))))

(defn clean-non-project-classes [project]
  (when (:clean-non-project-classes project)
    (doseq [f (file-seq (file (:compile-path project)))
            :when (and (.isFile f)
                       (not (whitelisted-class? project f))
                       (blacklisted-class? project f))]
      (.delete f))))

 ;; actual task

(defn- status [code msg]
  (when-not *silently* ; TODO: should silently only affect success?
    (binding [*out* (if (zero? code) *out* *err*)]
      (println msg)))
  code)

(def ^{:private true} success (partial status 0))
(def ^{:private true} failure (partial status 1))

(defn compile
  "Compile Clojure source into .class files.

Uses the namespaces specified under :aot in project.clj or those given
as command-line arguments."
  ([project]
     (when (:java-source-path project)
       (javac project))
     (if (seq (compilable-namespaces project))
       (if-let [namespaces (seq (stale-namespaces project))]
         (binding [*skip-auto-compile* true]
           (try
             (if (zero? (eval-in-project project
                                         `(doseq [namespace# '~namespaces]
                                            (when-not ~*silently*
                                              (println "Compiling" namespace#))
                                            (clojure.core/compile namespace#))))
               (success "Compilation succeeded.")
               (failure "Compilation failed."))
             (finally (clean-non-project-classes project))))
         (success "All namespaces already :aot compiled."))
       (success "No namespaces to :aot compile listed in project.clj.")))
  ([project & namespaces]
     (compile (assoc project
                :aot (if (= namespaces [":all"])
                       :all
                       (map symbol namespaces))))))
