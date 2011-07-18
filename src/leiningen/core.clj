(ns leiningen.core
  (:use [leiningen.util.ns :only [namespaces-matching]]
        [clojure.walk :only [walk]]
        [robert.hooke :only [add-hook]]
        [clojure.java.io :only [file]])
  (:require [leiningen.util.paths :as paths])
  (:import (java.io File)
           (org.apache.maven.artifact.versioning DefaultArtifactVersion)))

(def *interactive?* false)

(defmacro defdeprecated [old new]
  `(let [new# ~(str (.getName (:ns (meta (resolve new)))) "/" (name new))
         warn# (delay (println "Warning:" '~old "is deprecated; use" new#))]
     (defn ~(vary-meta old assoc :doc (format "Compatibility alias for %s" new))
       [& args#]
       (force warn#)
       (apply ~(resolve new) args#))))

(defdeprecated home-dir paths/leiningen-home)

(defdeprecated ns->path paths/ns->path)

(defdeprecated normalize-path paths/normalize-path)

(defn no-dev?
  "Should the dev dependencies and directories be ignored?

Warning: alpha; subject to change."
  []
  (System/getenv "LEIN_NO_DEV"))

(defn user-init
  "Load the user's ~/.lein/init.clj file, if present."
  []
  (let [init-file (File. (paths/leiningen-home) "init.clj")]
    (when (.exists init-file)
      (load-file (.getAbsolutePath init-file)))))

(defn user-settings
  "Look up the settings map from init.clj or an empty map if it doesn't exist."
  []
  (if-let [settings-var (resolve 'user/settings)]
    @settings-var
    {}))

;;; defproject

(def ^{:private true} project nil)

(defn- unquote-project [args]
  (walk (fn [item]
          (cond (and (seq? item) (= `unquote (first item))) (second item)
                ;; needed if we want fn literals to be usable by eval-in-project
                (or (seq? item) (symbol? item)) (list 'quote item)
                :else (unquote-project item)))
        identity
        args))

(defmacro defproject [project-name version & args]
  ;; This is necessary since we must allow defproject to be eval'd in
  ;; any namespace due to load-file; we can't just create a var with
  ;; def or we would not have access to it once load-file returned.
  `(let [m# (apply hash-map ~(cons 'list (unquote-project args)))
         root# ~(.getParent (File. *file*))
         normalize-path# (partial ~paths/normalize-path root#)]
     (alter-var-root #'project
                     (fn [_#] (assoc m#
                               :name ~(name project-name)
                               :group ~(or (namespace project-name)
                                           (name project-name))
                               :version ~version
                               :dependencies (or (:dependencies m#)
                                                 (:deps m#))
                               :dev-dependencies (or (:dev-dependencies m#)
                                                     (:dev-deps m#))
                               :compile-path (normalize-path#
                                              (or (:compile-path m#) "classes"))
                               :source-path (normalize-path#
                                             (or (:source-path m#) "src"))
                               :library-path (normalize-path#
                                              (or (:library-path m#) "lib"))
                               :test-path (normalize-path#
                                           (or (:test-path m#) "test"))
                               :resources-path (normalize-path#
                                                (or (:resources-path m#)
                                                    "resources"))
                               :dev-resources-path
                               (normalize-path# (or (:dev-resources-path m#)
                                                    (:test-resources-path m#)
                                                    "test-resources"))
                               ;; TODO: remove in 2.0
                               :test-resources-path
                               (normalize-path# (or (:dev-resources-path m#)
                                                    (:test-resources-path m#)
                                                    "test-resources"))
                               :native-path (normalize-path#
                                             (:native-path m# "native"))
                               :target-dir (normalize-path#
                                            (or (:target-dir m#) (:jar-dir m#)
                                                root#))
                               ;; TODO: remove in 2.0
                               :jar-dir (normalize-path#
                                         (or (:target-dir m#) (:jar-dir m#)
                                             root#))
                               :jar-exclusions (:jar-exclusions m# [#"^\."])
                               :uberjar-exclusions (:uberjar-exclusions
                                                    m# [#"^META-INF/DUMMY.SF"])
                               :root root#)))
     (when (:test-resources-path m#)
       (println (str "WARNING: :test-resources-path is deprecated; use "
                     ":dev-resources-path.")))
     (when (:jar-dir m#)
       (println (str "WARNING: :jar-dir is deprecated; use "
                     ":target-dir.")))
     #'project))

(defn read-project
  ([file]
     (try (binding [*ns* (the-ns 'leiningen.core)]
            (load-file file))
          project
          (catch java.io.FileNotFoundException _)))
  ([] (read-project "project.clj")))

(def default-repos {"central" {:url "http://repo1.maven.org/maven2"
                               :snapshots false}
                    ;; TODO: possibly separate releases/snapshots in 2.0.
                    "clojars" {:url "http://clojars.org/repo/"}})

(defn- init-settings [id settings]
  (cond (string? settings) {:url settings}
        ;; infer snapshots/release policy from repository id
        (= "releases" id) (merge {:snapshots false} settings)
        (= "snapshots" id) (merge {:releases false} settings)
        :else settings))

(defn repositories-for
  "Return a map of repositories including or excluding defaults."
  [project]
  (merge (when-not (:omit-default-repositories project)
           default-repos)
         (into {} (for [[id settings] (:repositories project)]
                    [id (init-settings id settings)]))))

(defn exit
  "Call System/exit. Defined as a function so that rebinding is possible."
  ([code]
     (shutdown-agents)
     (System/exit code))
  ([] (exit 0)))

(defn abort
  "Print msg to standard err and exit with a value of 1."
  [& msg]
  (binding [*out* *err*]
    (apply println msg)
    (exit 1)))

;;; Task execution

(def aliases (atom {"--help" "help" "-h" "help" "-?" "help" "-v" "version"
                    "--version" "version" "überjar" "uberjar" "cp" "classpath"
                    "int" "interactive"}))

(defn task-not-found [& _]
  (abort "That's not a task. Use \"lein help\" to list all tasks."))

(defn resolve-task
  ([task not-found]
     (let [task-ns (symbol (str "leiningen." task))
           task (symbol task)]
       (try
         (when-not (find-ns task-ns)
           (require task-ns))
         (or (ns-resolve task-ns task)
             not-found)
         (catch java.io.FileNotFoundException e
           not-found))))
  ([task] (resolve-task task #'task-not-found)))

(defn- hook-namespaces [project]
  (sort (concat (or (:hooks project)
                    (and (:implicit-hooks project)
                         (namespaces-matching "leiningen.hooks")))
                (:hooks (user-settings)))))

(defn- load-hooks [project]
  (doseq [n (hook-namespaces project)]
    (try (require n)
         (catch Exception e
           (when-not (empty? (.list (File. "lib")))
             (println "Warning: problem requiring" n "hook:" (.getMessage e))
             (when (System/getenv "DEBUG")
               (.printStackTrace e)))))))

(defn arglists [task-name]
  (:arglists (meta (resolve-task task-name))))

(defn- project-needed? [parameters]
  (if (vector? parameters)
    (= 'project (first parameters))
    (every? project-needed? parameters)))

(defn- project-accepted? [parameters]
  (and (first parameters)
       (re-find #"^project($|-or-)" (name (first parameters)))))

(defn- arg-count [parameters project]
  (if (and project (project-accepted? parameters))
    (dec (count parameters))
    (count parameters)))

(defn matching-arity? [task-name project args]
  (some (fn [parameters]
          (and (if (= '& (last (butlast parameters)))
                 (>= (count args) (- (arg-count parameters project) 2))
                 (= (arg-count parameters project) (count args)))
               (or project (not (project-needed? parameters)))
               parameters))
        ;; use project.clj if possible
        (reverse (sort-by count (arglists task-name)))))

(defn apply-task [task-name project args not-found]
  (let [task (resolve-task task-name not-found)]
    (if-let [parameters (matching-arity? task-name project args)]
      (if (project-accepted? parameters)
        (apply task project args)
        (apply task args))
      (let [args (arglists task-name)]
        (if (and (not project) (project-needed? args))
          (abort "Couldn't find project.clj, which is needed for" task-name)
          (abort "Wrong number of arguments to" task-name "task."
                 "\nExpected" args))))))

(defn prepend-tasks
  "Add a hook to target-var to run tasks-to-add, which must be tasks
  taking a project argument and nothing else."
  [target-var & tasks-to-add]
  (add-hook target-var (fn [target project & args]
                         (doseq [t tasks-to-add]
                           (t project))
                         (apply target project args))))

(def arg-separator ",")

(defn- append-to-group [groups arg]
  (update-in groups [(dec (count groups))] conj arg))

(defn make-groups
  ([args]
     (reduce make-groups [[]] args))
  ;; This could be a separate defn, but I can't think of a good name for it...
  ([groups arg]
     (if (.endsWith arg arg-separator)
       (-> groups
           (append-to-group (apply str (butlast arg)))
           (conj []))
       (append-to-group groups arg))))

(defn version-greater-eq?
  "Check if v1 is greater than or equal to v2, where args are version strings.
Takes major, minor and incremental versions into account."
  [v1 v2]
  (>= (.compareTo (DefaultArtifactVersion. v1)
                  (DefaultArtifactVersion. v2))
      0))

(defn verify-min-version
  [project]
  (when-not (version-greater-eq? (System/getenv "LEIN_VERSION")
                                 (:min-lein-version project))
    (do (println (str "\n*** Warning: This project requires Leiningen version "
                      (:min-lein-version project)
                      " ***"
                      "\n*** Using version " (System/getenv "LEIN_VERSION")
                      " could cause problems. ***\n"
                      "\n- Get the latest verison of Leiningen at\n"
                      "- https://github.com/technomancy/leiningen\n"
                      "- Or by executing \"lein upgrade\"\n\n")))))

(defn -main
  ([task-name & args]
     (user-init)
     (let [task-name (or (@aliases task-name) task-name "help")
           project (if (.exists (File. "project.clj")) (read-project))
           compile-path (:compile-path project)]
       (when (:min-lein-version project)
         (verify-min-version project))
       (when compile-path (.mkdirs (File. compile-path)))
       (binding [*compile-path* compile-path]
         (when project
           (load-hooks project))
         (apply-task task-name project args task-not-found))))
  ([]
     (doseq [[task & args] (make-groups *command-line-args*)
             :let [result (apply -main (or task "help") args)]]
       (when (and (integer? result) (pos? result))
         (exit result)))
     (exit 0)))
