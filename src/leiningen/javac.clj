(ns leiningen.javac
  "Compile Java source files."
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.io.File
           javax.tools.ToolProvider))

(defn- stale-java-sources
  "Returns a lazy seq of file paths: every Java source file within dirs modified
  since it was most recently compiled into compile-path."
  [dirs compile-path]
  (for [dir dirs
        ^File source (filter #(-> ^File % (.getName) (.endsWith ".java"))
                             (file-seq (io/file dir)))
        :let [rel-source (.substring (.getPath source) (inc (count dir)))
              rel-compiled (.replaceFirst rel-source "\\.java$" ".class")
              compiled (io/file compile-path rel-compiled)]
        :when (>= (.lastModified source) (.lastModified compiled))]
    (.getPath source)))

(def ^:private special-ant-javac-keys
  "Legacy (Lein1/Ant task) javac options that do not translate to the new (JDK's
  javac) format as key-value pairs. For example, :debug \"off\" needs to be
  translated to -g:none."
  [:destdir :debug :debugLevel])

(defn- normalize-specials
  "Handles legacy (Lein1/Ant task) javac options that do not translate to the
  new (JDK's javac) format as key-value pairs."
  [{:keys [debug debugLevel]}]
  ;; debug "off"               => -g:none
  ;; debugLevel "source,lines" => -g:source-lines
  (if (or (= "off" debug) (false? debug))
    ["-g:none"]
    (if debugLevel
      [(str "-g:" debugLevel)]
      [])))

(defn normalize-javac-options
  "Converts :javac-opts in Leiningen 1 format (passed as a map) into Leiningen 2
  format (a vector). Options in Leiningen 2 format are returned unmodified."
  [opts]
  (if (map? opts)
    (let [special-opts (select-keys opts special-ant-javac-keys)
          other-opts   (apply dissoc opts special-ant-javac-keys)
          specials     (normalize-specials special-opts)
          others       (mapcat (fn [[k v]] [(str "-" (name k)) v]) other-opts)]
      (vec (map (comp name str) (concat specials others))))
    opts))

(defn- safe-quote [s]
  (str "\"" (string/replace s "\\" "\\\\") "\""))

;; Tool's .run method expects the last argument to be an array of
;; strings, so that's what we'll return here.
(defn- javac-options
  "Compile all sources of possible options and add important defaults.
  Result is a String java array of options."
  [project files args]
  (let [options-file (File/createTempFile ".leiningen-cmdline" nil)]
    (.deleteOnExit options-file)
    (with-open [options-file (io/writer options-file)]
      (doto options-file
        (.write (format "-cp %s\n" (safe-quote (classpath/get-classpath-string project))))
        (.write (format "-d %s\n" (safe-quote (:compile-path project))))
        (.write (string/join "\n" (map safe-quote files)))))
    (into-array String
                (concat (normalize-javac-options (:javac-options project))
                        args
                        [(str "@" options-file)]))))

;; Pure java projects will not have Clojure on the classpath. As such, we need
;; to add it if it's not already there.
(def subprocess-profile
  {:dependencies [^:displace ['org.clojure/clojure (clojure-version)]]
   :eval-in :subprocess})

(defn- subprocess-form
  "Creates a form for running javac in a subprocess."
  [compile-path files javac-opts]
  (main/debug "Running javac with" javac-opts)
  `(let [abort# (fn [& msg#]
                  (.println java.lang.System/err (apply str msg#))
                  (java.lang.System/exit 1))]
     (if-let [compiler# (javax.tools.ToolProvider/getSystemJavaCompiler)]
       (do
         ~(when main/*info*
            `(binding [*out* *err*]
               (println "Compiling" ~(count files) "source files to" ~compile-path)))
         (utils/mkdirs (clojure.java.io/file ~compile-path))
         (when-not (zero?
                    (.run compiler# nil nil nil
                          (into-array java.lang.String ~javac-opts)))
           (abort# "Compilation of Java sources(lein javac) failed.")))
       (abort# "Java compiler not found; Be sure to use java from a JDK\n"
               "rather than a JRE by modifying PATH or setting JAVA_CMD."))))

(defn javac-project-for-subprocess
  "Merge profiles to create project appropriate for javac subprocess.  This
  function is mostly extracted to simplify testing, to validate that settings
  like `:local-repo` and `:mirrors` are respected."
  [project subprocess-profile]
  (-> (project/merge-profiles project [subprocess-profile])
      (project/retain-whitelisted-keys project)))

;; We can't really control what is printed here. We're just going to
;; allow `.run` to attach in, out, and err to the standard streams. This
;; should have the effect of compile errors being printed. javac doesn't
;; actually output any compilation info unless it has to (for an error)
;; or you make it do so with `-verbose`.
(defn- run-javac-subprocess
  "Run javac to compile all source files in the project. The compilation is run
  in a subprocess to avoid it from adding the leiningen standalone to the
  classpath, as leiningen adds itself to the classpath through the
  bootclasspath."
  [project args]
  (let [compile-path (:compile-path project)
        files (stale-java-sources (:java-source-paths project) compile-path)
        javac-opts (vec (javac-options project files args))
        form (subprocess-form compile-path files javac-opts)]
    (when (seq files)
      (try
        (binding [eval/*pump-in* false]
          (eval/eval-in
           (javac-project-for-subprocess project subprocess-profile)
           form))
        (catch Exception e
          (if-let [exit-code (:exit-code (ex-data e))]
            (main/exit exit-code)
            (throw e)))))))

(defn javac
  "Compile Java source files.

Add a :java-source-paths key to project.clj to specify where to find them.
Options passed in on the command line as well as options from the :javac-opts
vector in project.clj will be given to the compiler; e.g. `lein javac -verbose`.

Like the compile and deps tasks, this should be invoked automatically when
needed and shouldn't ever need to be run by hand. By default it is called before
compilation of Clojure source; change :prep-tasks to alter this."
  [project & args]
  (run-javac-subprocess project args))
