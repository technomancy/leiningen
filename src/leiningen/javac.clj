(ns leiningen.javac
  "Compile Java source files."
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io BufferedReader File FileReader]
           java.util.regex.Matcher
           javax.tools.ToolProvider))

(defn- rebuild-line
  "\"Rebuilds\" a line of Java, so that the comments aren't present."
  [start-line from-comment-block?]
  (loop [rest-line                 start-line
         in-comment?               from-comment-block?
         ^StringBuilder return-val (StringBuilder.)]
    (cond (empty? rest-line)
          [(.toString return-val) in-comment?]
          in-comment?
          (if (and (>= (count rest-line) 2)
                   (= (first rest-line) \*)
                   (= (first (rest rest-line)) \/))
            (recur (rest (rest rest-line))
                   false
                   return-val)
            (recur (rest rest-line)
                   true
                   return-val))
          (and (>= (count rest-line) 2)
               (= (first rest-line) \/)
               (= (first (rest rest-line)) \/))
          [(.toString return-val) false]
          (and (>= (count rest-line) 2)
               (= (first rest-line) \/)
               (= (first (rest rest-line)) \*))
          (recur (rest (rest rest-line)) ;; "eat" the asterisk so that
                 ;; /*/ doesn't happen
                 true
                 return-val)
          :else
          (recur (rest rest-line)
                 false
                 (.append return-val (first rest-line))))))

(defn uncommented-lines-from-reader
  "Returns a lazy sequence of lines from a Java character source
  with the comments stripped out. Closes the reader when there is
  no more input to read."
  ([reader] (uncommented-lines-from-reader reader false))
  ([^BufferedReader reader in-comment-block?]
   (let [next-line (.readLine reader)]
     (cond (nil? next-line)
           (do (.close reader)
               nil)
           in-comment-block?
           (if-not (.contains next-line "*/")
             (lazy-seq (cons "" (uncommented-lines-from-reader reader true)))
             (let [[rebuilt-line started-comment-block?]
                   (rebuild-line next-line true)]
               (lazy-seq (cons rebuilt-line
                               (uncommented-lines-from-reader
                                reader
                                started-comment-block?)))))
           :else
           (if (and (not (.contains next-line "/*"))
                    (not (.contains next-line "//")))
             (lazy-seq (cons next-line
                             (uncommented-lines-from-reader reader false)))
             (let [[rebuilt-line started-comment-block?]
                   (rebuild-line next-line false)]
               (lazy-seq (cons rebuilt-line
                               (uncommented-lines-from-reader
                                reader
                                started-comment-block?)))))))))

(defn- uncommented-lines-from-file
  "Returns a lazy sequence of lines from a Java file with
  the comments stripped out."
  [^File file]
  ;; uncommented-lines-from-reader has to .close the reader, because
  ;; it's a lazy-seq.
  (uncommented-lines-from-reader (-> file (FileReader.) (BufferedReader.))
                                 false))

(defn extract-package-from-line-seq
  "Extracts a package declaration from a seq of Java source lines.
  The seq must be stripped of all comments before being passed
  to this function; use uncommented-lines-from-* for that."
  [line-seq]
  ;; In accordance with the Java specification, package declarations are only
  ;; legal if they are the first non-comment, non-whitespace element of the
  ;; file. So, we can speed up processing of large files by only checking the
  ;; first few tokens.
  (let [nonempty-seq (filter #(not (empty? %))
                             (map string/trim line-seq))]
    (if (empty? nonempty-seq)
      nil
      (let [first-line (string/split (first nonempty-seq) #"\s+")]
        (cond (not= (first first-line) "package")
              ;; No package declarations
              nil
              (second first-line)
              ;; Package name just after "package" token
              ;; Usually. If the result is "package ;" then you end up
              ;; with an empty string.
              (string/replace (second first-line) ";" "")
              (nil? (second nonempty-seq))
              ;; Dangling "package" token in a file
              nil
              :else
              ;; Assume that the immediate successor is the package name
              (-> (second nonempty-seq)
                  (string/split #"\s+")
                  (first)
                  (string/replace ";" "")))))))

;; For folks who don't follow source directory convention, and also for the
;; purpose of allowing partial source builds, we have to extract the
;; child path in the compile-path dir from the package declaration.
;; This assumes that the Java code is syntactically correct, of course,
;; but if the code won't even compile in the first place, it won't matter
;; that you can't find the previous compiled version.
(defn- extract-compiled-path
  "Generates an \"effective\" compiled path for a given .java file.

  In order to do this, the passed file is opened, and scanned for the
  first valid package declaration. The package declaration is then transformed
  into a relative directory path.

  This function assumes that javac -d has been used, which will generate
  a directory structure in line with the package declaration. The resulting
  return value will be relative to the argument to javac -d."
  [^File file]
  (let [extracted-package (-> file
                              (uncommented-lines-from-file)
                              (extract-package-from-line-seq))]
    (if (nil? extracted-package)
      (.replaceFirst (.getName file) "\\.java$" ".class")
      (str (string/replace extracted-package "." (File/separator))
           (File/separator)
           (.replaceFirst (.getName file) "\\.java$" ".class")))))

(defn- stale-java-sources
  "Returns a lazy seq of file paths: every Java source file within dirs modified
  since it was most recently compiled into compile-path."
  [compile-path dirs]
  (for [dir dirs
        ^File source (filter #(-> ^File % (.getName) (.endsWith ".java"))
                             (file-seq (io/file dir)))
        :let [rel-compiled (extract-compiled-path source)
              compiled     (io/file compile-path rel-compiled)]
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
                        (filter #(or (string/starts-with? % "-")
                                     (and (string/starts-with? % "@")
                                          (not (string/starts-with? % "@@"))))
                                args)
                        [(str "@" options-file)]))))

;; seq the map so that an empty sequence returns nil
;; we'll use this later to see if we have any source paths in the args
(defn- source-paths-from-args [args]
  (seq (map #(if (string/starts-with? % "@@") (.substring 1 %) %)
            (filter #(or (and (not (string/starts-with? % "-"))
                              (not (string/starts-with? % "@")))
                         (string/starts-with? % "@@"))
                    args))))

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
         (binding [*out* *err*]
           (println "Compiling" ~(count files) "source files to" ~compile-path))
         (.mkdirs (clojure.java.io/file ~compile-path))
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
        ;; :java-source-paths is absoluteized before we get to it
        ;; but our args aren't, so remember to add the project root
        ;; to them.
        files (stale-java-sources compile-path
                                  (if-let [argfs (source-paths-from-args args)]
                                    (map #(str (:root project)
                                               (File/separator)
                                               %)
                                         argfs)
                                    (:java-source-paths project)))
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
Options passed in on the command line which start with '-' or '@' will be
treated as arguments to the compiler, and all arguments in the :javac-opts
vector in project.clj will be directly given to the compiler; e.g.
`lein javac -verbose`. Otherwise, options will be treated as Java source
directories (relative to the project root) for the current invocation, 
overriding the values in the :java-source-path vector.

Like the compile and deps tasks, this should be invoked automatically when
needed and shouldn't ever need to be run by hand. By default it is called before
compilation of Clojure source; change :prep-tasks to alter this."
  [project & args]
  (run-javac-subprocess project args))
