(ns leiningen.compile
  "Compile the namespaces listed in project.clj or all namespaces in src."
  (:require lancet)
  (:use  [leiningen.deps :only [deps]]
         [leiningen.core :only [ns->path]]
         [leiningen.javac :only [javac]]
         [leiningen.classpath :only [make-path find-lib-jars get-classpath]]
         [clojure.java.io :only [file]]
         [leiningen.util.ns :only [namespaces-in-dir]])
  (:refer-clojure :exclude [compile])
  (:import (org.apache.tools.ant.taskdefs Java)
           (java.lang.management ManagementFactory)
           java.util.regex.Pattern
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
    (if (:main project)
      (conj nses (:main project))
      nses)))

(defn stale-namespaces
  "Return a seq of namespaces that are both compilable and that have missing or
  out-of-date class files."
  [project]
  (filter
   (fn [n]
     (let [clj-path (ns->path n)
           class-file (file (:compile-path project)
                            (.replace clj-path "\\.clj" "__init.class"))]
       (or (not (.exists class-file))
           (> (.lastModified (file (:source-path project) clj-path))
              (.lastModified class-file)))))
   (compilable-namespaces project)))

(defn get-by-pattern
  "Gets a value from map m, but uses the keys as regex patterns, trying
   to match against k instead of doing an exact match."
  [m k]
  (m (first (drop-while #(nil? (re-find (re-pattern %) k))
                        (keys m)))))

(def native-names
     {"Mac OS X" :macosx
      "Windows" :windows
      "Linux" :linux
      "FreeBSD" :freebsd
      "SunOS" :solaris
      "OpenBSD" :openbsd
      "amd64" :x86_64
      "x86_64" :x86_64
      "x86" :x86
      "i386" :x86
      "arm" :arm
      "sparc" :sparc})

(defn get-os
  "Returns a keyword naming the host OS."
  []
  (get-by-pattern native-names (System/getProperty "os.name")))

(defn get-arch
  "Returns a keyword naming the host architecture"
  []
  (get-by-pattern native-names (System/getProperty "os.arch")))

(defn- find-native-lib-path
  "Returns a File representing the directory where native libs for the
  current platform are located."
  [project]
  (when (and (get-os) (get-arch))
    (let [osdir (name (get-os))
          archdir (name (get-arch))
          f (file "native" osdir archdir)]
      (if (.exists f)
        f
        nil))))

(defn- get-jvm-args
  [project]
  (concat (.getInputArguments (ManagementFactory/getRuntimeMXBean))
          (:jvm-opts project)))

(defn get-readable-form [java project form init]
  (let [cp (str (.getClasspath (.getCommandLine java)))
        form `(do ~init
                  (def ~'*classpath* ~cp)
                  (set! ~'*warn-on-reflection*
                        ~(:warn-on-reflection project))
                  ~form)]
    ;; work around java's command line handling on windows
    ;; http://bit.ly/9c6biv This isn't perfect, but works for what's
    ;; currently being passed; see
    ;; http://www.perlmonks.org/?node_id=300286 for some of the
    ;; landmines involved in doing it properly
    (if (= (get-os) :windows)
      (pr-str (pr-str form))
      (prn-str form))))

;; TODO: split this function up
(defn eval-in-project
  "Executes form in an isolated classloader with the classpath and compile path
  set correctly for the project. Pass in a handler function to have it called
  with the java task right before executing if you need to customize any of its
  properties (classpath, library-path, etc)."
  [project form & [handler skip-auto-compile init]]
  (when skip-auto-compile
    (println "WARNING: eval-in-project's skip-auto-compile arg is deprecated."))
  (when (and (not (or *skip-auto-compile* skip-auto-compile))
             (empty? (.list (file (:compile-path project)))))
    (binding [*silently* true]
      (compile project)))
  (when (empty? (find-lib-jars project))
    (deps project))
  (if (:eval-in-leiningen project)
    (do ;; bootclasspath workaround: http://dev.clojure.org/jira/browse/CLJ-673
      (require '[clojure walk repl])
      (require '[clojure.java io shell browse])
      ;; need to at least pretend to return an exit code
      (eval form) 0)
    (let [java (Java.)
          native-path (or (:native-path project)
                          (find-native-lib-path project))]
      (.setProject java lancet/ant-project)
      (.addSysproperty java (doto (Environment$Variable.)
                              (.setKey "clojure.compile.path")
                              (.setValue (:compile-path project))))
      (when native-path
        (.addSysproperty java (doto (Environment$Variable.)
                                (.setKey "java.library.path")
                                (.setValue (cond
                                            (= file (class native-path))
                                            (.getAbsolutePath native-path)
                                            (fn? native-path) (native-path)
                                            :default native-path)))))
      (.setClasspath java (apply make-path (get-classpath project)))
      (.setFailonerror java true)
      (.setFork java true)
      (doseq [arg (get-jvm-args project)]
        (when-not (re-matches #"^-Xbootclasspath.+" arg)
          (.setValue (.createJvmarg java) arg)))
      (.setClassname java "clojure.main")
      ;; to allow plugins and other tasks to customize
      (when handler
        (println "WARNING: eval-in-project's handler argument is deprecated.")
        (handler java))
      (.setValue (.createArg java) "-e")
      (.setValue (.createArg java) (get-readable-form java project form init))
      (.executeJava java))))

(defn- platform-nullsink []
  (file (if (= :windows (get-os))
          "NUL"
          "/dev/null")))

(defn- has-source-package?
  "Test if the class file's package exists as a directory in :source-path."
  [project f]
  (.isDirectory (file (.replace (.getParent f)
                                (:compile-path project)
                                (:source-path project)))))

(defn delete-non-project-classes [project]
  (when (and (not= :all (:aot project))
             (not (:keep-non-project-classes project)))
    (doseq [f (file-seq (file (:compile-path project)))
            :when (and (.isFile f) (not (has-source-package? project f)))]
      (.delete f))))

(defn- status [code msg]
  (when-not *silently*
    (.write (if (zero? code) *out* *err*) (str msg "\n")))
  code)

(def ^{:private true} success (partial status 0))
(def ^{:private true} failure (partial status 1))

(defn compile
  "Ahead-of-time compile the namespaces given under :aot in project.clj or
those given as command-line arguments."
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
             (finally (delete-non-project-classes project))))
         (success "All namespaces already :aot compiled."))
       (success "No namespaces to :aot compile listed in project.clj.")))
  ([project & namespaces]
     (compile (assoc project
                :aot (if (= namespaces [":all"])
                       :all
                       (map symbol namespaces))))))
