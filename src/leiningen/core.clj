(ns leiningen.core
  (:use [clojure.contrib.find-namespaces :only [find-namespaces-on-classpath]]
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

(defn read-project
  ([file]
     (try
      (load-file file)
      project
      (catch java.io.FileNotFoundException _
        (abort "No project.clj found in this directory."))))
  ([] (read-project "project.clj")))

(def aliases (atom {"--help" "help" "-h" "help" "-?" "help" "-v" "version"
                    "--version" "version" "überjar" "uberjar"}))

(def no-project-needed (atom #{"new" "help" "version"}))

(defn task-not-found [& _]
  (abort "That's not a task. Use \"lein help\" to list all tasks."))

(defn resolve-task [task]
  (let [task-ns (symbol (str "leiningen." task))
        task (symbol task)]
    (try
     (when-not (find-ns task-ns)
       (require task-ns))
     (or (ns-resolve task-ns task)
         #'task-not-found)
     (catch java.io.FileNotFoundException e
       #'task-not-found))))

(defn- load-hooks []
  (doseq [n (sort (find-namespaces-on-classpath))
          :when (re-find #"^leiningen\.hooks\." (name n))]
    (try (require n)
         (catch Exception e
           (when-not (empty? (.list (File. "lib")))
             (println "Problem loading hooks:" n (.getMessage e)))))))

(defn ns->path [n]
  (str (.. (str n)
           (replace \- \_)
           (replace \. \/))
       ".clj"))

(defn path->ns [path]
  (.. (.replaceAll path "\\.clj" "")
      (replace \_ \-)
      (replace \/ \.)))

(defn -main
  ([& [task-name & args]]
     (let [task-name (or (@aliases task-name) task-name "help")
           args (if (@no-project-needed task-name)
                  args
                  (conj args (read-project)))
           compile-path (:compile-path (first args))]
       (when compile-path (.mkdirs (File. compile-path)))
       (binding [*compile-path* compile-path]
         (load-hooks)
         ;; TODO: can we catch only task-level arity problems here?
         ;; compare args and (:arglists (meta (resolve-task task)))?
         (let [task (resolve-task task-name)
               value (apply task args)]
           (when (integer? value)
             (System/exit value))))))
  ([] (apply -main (or *command-line-args* ["help"]))))
