(ns leiningen.core
  (:use [clojure.contrib.find-namespaces :only [find-namespaces-on-classpath]]
        [clojure.string :only [split]]
        [clojure.walk :only [walk]])
  (:import [java.io File])
  (:gen-class))

(def project nil)

(defn- unquote-project [args]
  (walk (fn [item]
          (cond (and (seq? item) (= `unquote (first item))) (second item)
                (symbol? item) (list 'quote item)
                :else (unquote-project item)))
        identity
        args))

(defmacro defproject [project-name version & args]
  ;; This is necessary since we must allow defproject to be eval'd in
  ;; any namespace due to load-file; we can't just create a var with
  ;; def or we would not have access to it once load-file returned.
  `(do
     (let [m# (apply hash-map ~(cons 'list (unquote-project args)))
           root# ~(.getParent (File. *file*))]
       (alter-var-root #'project
                       (fn [_#] (assoc m#
                                  :name ~(name project-name)
                                  :group ~(or (namespace project-name)
                                              (name project-name))
                                  :version ~version
                                  :compile-path (or (:compile-path m#)
                                                    (str root# "/classes"))
                                  :source-path (or (:source-path m#)
                                                   (str root# "/src"))
                                  :library-path (or (:library-path m#)
                                                    (str root# "/lib"))
                                  :test-path (or (:test-path m#)
                                                 (str root# "/test"))
                                  :resources-path (or (:resources-path m#)
                                                      (str root# "/resources"))
                                  :test-resources-path
                                  (or (:test-resources-path m#)
                                      (str root# "/test-resources"))
                                  :jar-dir (or (:jar-dir m#) root#)
                                  :root root#))))
     (def ~(symbol (name project-name)) project)))

(defn abort [msg]
  (println msg)
  (System/exit 1))

(defn get-home-dir
  "Returns full path to Lein home dir ($LEIN_HOME or $HOME/.lein) if it exists"
  []
  (let [lein-home-dir (System/getenv "LEIN_HOME")
        user-home-dir (or (System/getenv "HOME")
                          (System/getProperty "user.home"))
        home-dir (or lein-home-dir
                     (and user-home-dir (str (File. (str user-home-dir) ".lein"))))]
    (when home-dir
      (let [hdir (File. home-dir)]
        (when (.isDirectory hdir)
          (.getAbsolutePath hdir))))))

(defn get-global-init-script
  "Returns full path to global init script (init.clj) if it exists"
  []
  (when-let [home-dir (get-home-dir)]
    (let [init-file (File. (str home-dir) "init.clj")]
      (when (.isFile init-file)
        (.getAbsolutePath init-file)))))

(defn read-project
  ([file]
     (try
      (load-file file)
      project
      (catch java.io.FileNotFoundException _
        (abort "No project.clj found in this directory."))))
  ([] (read-project "project.clj")))

(def aliases (atom {"--help" "help" "-h" "help" "-?" "help" "-v" "version"
                    "--version" "version" "überjar" "uberjar"
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
                 (filter #(re-find #"^leiningen\.hooks\." (name %))
                         (find-namespaces-on-classpath))))))

(defn- load-hooks [project]
  (try (doseq [n (hook-namespaces project)]
         (require n))
       (catch Exception e
         (when-not (empty? (.list (File. "lib")))
           (println "Warning: problem requiring hooks:" (.getMessage e))
           (println "...continuing without hooks completely loaded.")))))

(defn ns->path [n]
  (str (.. (str n)
           (replace \- \_)
           (replace \. \/))
       ".clj"))

(defn path->ns [path]
  (.. (.replaceAll path "\\.clj" "")
      (replace \_ \-)
      (replace \/ \.)))

(defn arglists [task-name not-found]
  (:arglists (meta (resolve-task task-name not-found))))

(defn project-needed [task-name not-found]
  (some #{'project} (map first (arglists task-name not-found))))

(defn matching-arity [task-name project args not-found]
  (let [arg-count (if (project-needed task-name not-found)
                    (inc (count args))
                    (count args))]
    (some (fn [defined-args]
            (if (= '& (last (butlast defined-args)))
              (>= arg-count (- (count defined-args) 2))
              (= arg-count (count defined-args))))
      (arglists task-name not-found))))

(defn apply-task [task-name project args not-found]
  (let [task (resolve-task task-name not-found)]
    (if (matching-arity task-name project args not-found)
      (if (project-needed task-name not-found)
        (apply task project args)
        (apply task args))
      (not-found))))

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

(defn -main
  ([& [task-name & args]]
     (let [task-name (or (@aliases task-name) task-name "help")
           project (if (project-needed task-name task-not-found) (read-project))
           compile-path (:compile-path project)]
       (when compile-path (.mkdirs (File. compile-path)))
       (binding [*compile-path* compile-path]
         (when project
           (load-hooks project))
         (let [value (apply-task task-name project args task-not-found)]
           (when (integer? value)
             (System/exit value))))))
  ([]
     (doseq [arg-group (make-groups *command-line-args*)]
       (apply -main (or arg-group ["help"])))))
