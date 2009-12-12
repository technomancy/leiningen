(ns leiningen.core
  (:use [clojure.contrib.with-ns])
  (:import [java.io File])
  (:gen-class))

(def project nil)

(defmacro defproject [project-name version & args]
  ;; This is necessary since we must allow defproject to be eval'd in
  ;; any namespace due to load-file; we can't just create a var with
  ;; def or we would not have access to it once load-file returned.
  `(do
     (let [m# (apply hash-map (quote ~args))
           root# ~(.getParent (java.io.File. *file*))]
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
                                  :root root#))))
     (def ~(symbol (name project-name)) project)))

;; So it doesn't need to be fully-qualified in project.clj
(with-ns 'clojure.core (use ['leiningen.core :only ['defproject]]))

(defn exit-with-error [msg]
  (println msg)
  (System/exit 1))

;; TODO: prompt to run "new" if no project file is found
(defn read-project
  ([file]
     (try
      (load-file file)
      project
      (catch java.io.FileNotFoundException _
        (exit-with-error "No project.clj found in this directory."))))
  ([] (read-project "project.clj")))

(def aliases {"--help" "help" "-h" "help" "-?" "help"
              "-v" "version" "--version" "version"})

(def no-project-needed #{"new" "help" "version"})

(defn resolve-task [task]
  (let [task-ns (symbol (str "leiningen." task))
        task (symbol task)
        error-fn (fn [& _]
                   (exit-with-error
                     (format "%s is not a task. Use \"help\" to list all tasks."
                             task)))]
    (try
     (require task-ns)
     (or (ns-resolve task-ns task)
         error-fn)
     (catch java.io.FileNotFoundException e
       error-fn))))

(defn -main [& [task & args]]
  (let [task (or (aliases task) task "help")
        args (if (no-project-needed task)
               args
               (conj args (read-project)))
        compile-path (:compile-path (first args))]
    (when compile-path (.mkdirs (File. compile-path)))
    (binding [*compile-path* compile-path]
      (try
       (apply (resolve-task task) args)
       (catch IllegalArgumentException _
         (exit-with-error (format "Wrong number of arguments to task %s."
                                  task)))))
    ;; In case tests or some other task started any:
    (shutdown-agents)))
