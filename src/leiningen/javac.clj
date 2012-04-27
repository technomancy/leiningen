(ns leiningen.javac
  "Compile Java source files."
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.main :as main]
            [clojure.java.io :as io])
  (:import java.io.File
           javax.tools.ToolProvider))

(defn- stale-java-sources
  "Returns a lazy seq of file paths: every Java source file within
  dirs modified since it was most recently compiled into
  compile-path."
  [dirs compile-path]
  (for [dir dirs
        ^File source (filter #(-> ^File % (.getName) (.endsWith ".java"))
                             (file-seq (io/file dir)))
        :let [rel-source (.substring (.getPath source) (inc (count dir)))
              rel-compiled (.replaceFirst rel-source "\\.java$" ".class")
              compiled (io/file compile-path rel-compiled)]
        :when (>= (.lastModified source) (.lastModified compiled))]
    (.getPath source)))

(def ^{:private true
       :doc "Legacy (Lein1/Ant task) javac options that do not translate
             to the new (JDK's javac) format as key-value pairs. For example, :debug \"off\"
             needs to be translated to -g:none."}
  special-ant-javac-keys [:destdir :debug :debugLevel])

(defn- normalize-specials
  "Handles legacy (Lein1/Ant task) javac options that do not translate
   to the new (JDK's javac) format as key-value pairs"
  [{:keys [debug debugLevel]}]
  ;; debug "off"               => -g:none
  ;; debugLevel "source,lines" => -g:source-lines
  (if (or (= "off" debug) (false? debug))
    ["-g:none"]
    (if debugLevel
      [(str "-g:" debugLevel)]
      [])))

(defn normalize-javac-options
  "Converts :javac-opts in Leiningen 1 format (passed as a map) into
   Leiningen 2 format (a vector).
   Options in Leiningen 2 format are returned unmodified"
  [opts]
  (if (map? opts)
    (let [special-opts (select-keys opts special-ant-javac-keys)
          other-opts   (apply dissoc (concat [opts] special-ant-javac-keys))
          specials     (normalize-specials special-opts)
          others       (flatten (vec (map (fn [[k v]]
                                            [(str "-" (name k)) v]) other-opts)))]
      (vec (map (comp name str) (flatten (concat specials others)))))
    opts))

;; Tool's .run method expects the last argument to be an array of
;; strings, so that's what we'll return here.
(defn- javac-options
  "Compile all sources of possible options and add important defaults.
  Result is a String java array of options."
  [project files args]
  (into-array
   String
   (concat (normalize-javac-options (:javac-options project))
           args
           ["-cp" (classpath/get-classpath-string project)
            "-d" (:compile-path project)]
           files)))

;; We can't really control what is printed here. We're just going to
;; allow `.run` to attach in, out, and err to the standard streams. This
;; should have the effect of compile errors being printed. javac doesn't
;; actually output any compilation info unless it has to (for an error)
;; or you make it do so with `-verbose`.
(defn- run-javac-task
  "Run javac to compile all source files in the project."
  [project args]
  (let [compile-path (:compile-path project)
        files (stale-java-sources (:java-source-paths project) compile-path)]
    (when (seq files)
      (if-let [compiler (ToolProvider/getSystemJavaCompiler)]
        (do
          (main/info "Compiling" (count files) "source files to" compile-path)
          (.mkdirs (io/file compile-path))
          (.run compiler nil nil nil (javac-options project files args)))
        (main/abort "lein-javac: system java compiler not found;"
                    "a JDK (vs. JRE) install is required.")))))

(defn javac
  "Compile Java source files.

Add a :java-source-paths key to project.clj to specify where to find them.
Any options passed will be given to javac. One place where this can be useful
is `lein javac -verbose`."
  [project & args]
  (run-javac-task project args))
