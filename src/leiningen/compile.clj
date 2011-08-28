(ns leiningen.compile
  "Compile Clojure source into .class files."
  (:use [leiningen.deps :only [deps find-jars]]
        [leiningen.core :only [defdeprecated user-settings *interactive?*]]
        [leiningen.javac :only [javac]]
        [leiningen.classpath :only [make-path get-classpath]]
        [clojure.java.io :only [file resource reader]]
        [leiningen.util.ns :only [namespaces-in-dir]])
  (:require [leiningen.util.paths :as paths]
            [lancet.core :as lancet])
  (:refer-clojure :exclude [compile])
  (:import (java.io PushbackReader)
           (org.apache.tools.ant.taskdefs Java)
           (org.apache.tools.ant.types ZipFileSet)
           (java.lang.management ManagementFactory)
           (java.util.regex Pattern)
           (org.apache.tools.ant.types Environment$Variable)))

(declare compile)

(def *silently* false)

(def *skip-auto-compile* false)

(defn- regex?
  "Returns true if we have regex class"
  [str-or-re]
  (instance? java.util.regex.Pattern str-or-re))

(defn- separate
  "copy of separate function from c.c.seq-utils"
  [f s]
  [(filter f s) (filter (complement f) s) ])

(defn- find-namespaces-by-regex
  "Trying to generate list of namespaces, matching to given regexs"
  [project nses]
  (let [[res syms] (separate regex? nses)]
    (if (seq res)
      (set (for [re res n (namespaces-in-dir (:source-path project))
                 :when (re-find re (name n))]
             n))
      nses)))

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
    (if (and (:main project) (not (:skip-aot (meta (:main project)))))
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

(defdeprecated get-os paths/get-os)

(defdeprecated get-os paths/get-arch)

(defn platform-nullsink []
  (file (if (= :windows (paths/get-os))
          "NUL"
          "/dev/null")))

(defn- get-jvm-args [project]
  (remove empty?
          `(~@(when-let [opts (System/getenv "JVM_OPTS")] [opts])
            ~@(:jvm-opts project)
            ~@(:jvm-opts (user-settings)))))

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
                        (when-not (or (= "1.5" (System/getProperty
                                                "java.specification.version"))
                                      ~*interactive?*)
                          (shutdown-agents)))))]
    ;; work around java's command line handling on windows
    ;; http://bit.ly/9c6biv This isn't perfect, but works for what's
    ;; currently being passed; see
    ;; http://www.perlmonks.org/?node_id=300286 for some of the
    ;; landmines involved in doing it properly
    (if (and (= (paths/get-os) :windows) java)
      (pr-str (pr-str form))
      (prn-str form))))

(defn prep [project skip-auto-compile]
  (when (and (not (or *skip-auto-compile* skip-auto-compile))
             (empty? (.list (file (:compile-path project)))))
    (binding [*silently* true]
      (compile project)))
  (when (or (empty? (find-jars project))
            (:checksum-deps project))
    (deps project)))

(defn- add-system-property [java key value]
  (.addSysproperty java (doto (Environment$Variable.)
                          (.setKey (name key))
                          (.setValue (name value)))))

;; TODO: split this function up
(defn eval-in-project
  "Executes form in an isolated classloader with the classpath and compile path
  set correctly for the project. If the form depends on any requires, put them
  in the init arg to avoid the Gilardi Scenario: http://technomancy.us/143"
  [project form & [handler skip-auto-compile init]]
  (when skip-auto-compile
    (println "WARNING: eval-in-project's skip-auto-compile arg is deprecated."))
  (prep project skip-auto-compile)
  (if (:eval-in-leiningen project)
    (do ;; bootclasspath workaround: http://dev.clojure.org/jira/browse/CLJ-673
      (require '[clojure walk repl])
      (require '[clojure.java io shell browse])
      (when (:debug project)
        (System/setProperty "clojure.debug" "true"))
      ;; need to at least pretend to return an exit code
      (try (binding [*warn-on-reflection* (:warn-on-reflection project)
                     *ns* *ns*]
             (eval (read-string (get-readable-form nil project form init))))
           0
           (catch Exception e
             (.printStackTrace e)
             1)))
    (let [java (Java.)]
      (.setProject java lancet/ant-project)
      (add-system-property java :clojure.compile.path (:compile-path project))
      (add-system-property java (format "%s.version" (:name project))
                           (:version project))
      (when (:debug project)
        (add-system-property java :clojure.debug true))
      (when (.exists (file (:native-path project)))
        (add-system-property java "java.library.path" (:native-path project)))
      ;; TODO: remove in 2.0?
      (when (and (paths/legacy-native-path project)
                 (.exists (file (paths/legacy-native-path project))))
        (add-system-property java "java.library.path"
                             (str (paths/legacy-native-path project))))
      (.setClasspath java (apply make-path (get-classpath project)))
      (.setFailonerror java true)
      (.setFork java true)
      (.setDir java (file (:root project)))
      (doseq [arg (get-jvm-args project)]
        (.setValue (.createJvmarg java) arg))
      (.setClassname java "clojure.main")
      ;; to allow plugins and other tasks to customize
      (when handler
        (println "WARNING: eval-in-project's handler argument is deprecated.")
        (handler java))
      (.setValue (.createArg java) "-e")
      (.setValue (.createArg java) (get-readable-form java project form init))
      (.executeJava java))))

(defn- has-source-package?
  "Test if the class file's package exists as a directory in :source-path."
  [project f source-path]
  (and source-path (.isDirectory (file (.replace (.getParent f)
                                                 (:compile-path project)
                                                 source-path)))))

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

(defn- status [code msg]
  (when-not *silently*
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
     (.mkdir (file (:compile-path project)))
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
