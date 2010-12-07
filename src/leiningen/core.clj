(ns leiningen.core
  (:use [leiningen.util.ns :only [namespaces-matching]]
        [clojure.string :only [split]]
        [clojure.walk :only [walk]])
  (:import (java.io File)))

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
         normalize-path# (fn [p#] (when p# (.getPath (File. p#))))]
     (alter-var-root #'project
                     (fn [_#] (assoc m#
                               :name ~(name project-name)
                               :group ~(or (namespace project-name)
                                           (name project-name))
                               :version ~version
                               :compile-path (normalize-path#
                                              (or (:compile-path m#)
                                                  (str root# "/classes")))
                               :source-path (normalize-path#
                                             (or (:source-path m#)
                                                 (str root# "/src")))
                               :library-path (normalize-path#
                                              (or (:library-path m#)
                                                  (str root# "/lib")))
                               :test-path (normalize-path#
                                           (or (:test-path m#)
                                           (str root# "/test")))
                               :resources-path (normalize-path#
                                                (or (:resources-path m#)
                                                    (str root# "/resources")))
                               :dev-resources-path
                               (normalize-path#
                                (or (:dev-resources-path m#)
                                    (:test-resources-path m#)
                                    (str root# "/test-resources")))
                               ;; TODO: remove in 2.0
                               :test-resources-path
                               (normalize-path#
                                (or (:dev-resources-path m#)
                                    (:test-resources-path m#)
                                    (str root# "/test-resources")))
                               :target-dir (normalize-path#
                                            (or (:target-dir m#) (:jar-dir m#)
                                                root#))
                               ;; TODO: remove in 2.0
                               :jar-dir (normalize-path#
                                         (or (:target-dir m#) (:jar-dir m#)
                                             root#))
                               :root root#)))
     (when (:test-resources-path m#)
       (println (str "WARNING: :test-resources-path is deprecated; use "
                     ":dev-resources-path.")))
     (when (:jar-dir m#)
       (println (str "WARNING: :jar-dir is deprecated; use "
                     ":target-dir.")))
     (def ~(symbol (name project-name)) @#'project)))

(defn exit
  "Call System/exit. Defined as a function so that rebinding is possible."
  ([code]
     (System/exit code))
  ([] (exit 0)))

(defn abort [& msg]
  (binding [*out* *err*]
    (apply println msg)
    (exit 1)))

(defn home-dir
  "Returns full path to Lein home dir ($LEIN_HOME or $HOME/.lein) if it exists"
  []
  (.getAbsolutePath (doto (if-let [lein-home (System/getenv "LEIN_HOME")]
                            (File. lein-home)
                            (File. (System/getProperty "user.home") ".lein"))
                      .mkdirs)))

(def default-repos {"central" "http://repo1.maven.org/maven2"
                    "clojure" "http://build.clojure.org/releases"
                    "clojure-snapshots" "http://build.clojure.org/snapshots"
                    "clojars" "http://clojars.org/repo/"})

(defn repositories-for [project]
  (merge (when-not (:omit-default-repositories project) default-repos)
         (:repositories project)))

(defn read-project
  ([file]
     (try (load-file file)
          project
          (catch java.io.FileNotFoundException _)))
  ([] (read-project "project.clj")))

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
  (sort (or (:hooks project)
            (and (:implicit-hooks project)
                 (namespaces-matching "leiningen.hooks")))))

(defn- load-hooks [project]
  (try (doseq [n (hook-namespaces project)]
         (require n))
       (catch Exception e
         (when-not (empty? (.list (File. "lib")))
           (println "Warning: problem requiring hooks:" (.getMessage e))
           (when (System/getenv "DEBUG")
             (.printStackTrace e))
           (println "...continuing without hooks completely loaded.")))))

(defn user-init []
  (let [init-file (File. (home-dir) "init.clj")]
    (when (.exists init-file)
      (load-file (.getAbsolutePath init-file)))))

(defn ns->path [n]
  (str (.. (str n)
           (replace \- \_)
           (replace \. \/))
       ".clj"))

(defn path->ns [path]
  (.. (.replaceAll path "\\.clj" "")
      (replace \_ \-)
      (replace \/ \.)))

(defn arglists [task-name]
  (:arglists (meta (resolve-task task-name))))

(defn- project-needed? [parameters]
  (if (vector? parameters)
    (= 'project (first parameters))
    (every? project-needed? parameters)))

(defn- arg-count [parameters project]
  (if (and project (project-needed? parameters))
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
      (if (project-needed? parameters)
        (apply task project args)
        (apply task args))
      (let [args (arglists task-name)]
        (if (and (not project) (project-needed? args))
          (abort "Couldn't find project.clj, which is needed for" task-name)
          (abort "Wrong number of arguments to" task-name "task."
                 "\nExpected" args))))))

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
  (let [v1 (map #(Integer. %) (re-seq #"\d" (first (split v1 #"-" 2))))
        v2 (map #(Integer. %) (re-seq #"\d" (first (split v2 #"-" 2))))]
    (or (and (every? true? (map >= v1 v2))
             (>= (count v1) (count v2)))
        (every? true? (map > v1 v2)))))

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
  ([& [task-name & args]]
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
         (let [value (apply-task task-name project args task-not-found)]
           (when (integer? value)
             (exit value))))))
  ([]
     (doseq [[task & args] (make-groups *command-line-args*)
             :let [result (apply -main (or task "help") args)]]
       (when (and (number? result) (pos? result))
         (exit result)))))
