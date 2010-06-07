(ns leiningen.core
  (:use [clojure.contrib.with-ns]
        [clojure.contrib.find-namespaces :only [find-namespaces-on-classpath]])
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

(defn resolve-task [task]
  (let [task-ns (symbol (str "leiningen." task))
        task (symbol task)
        ;; code is expecting a var, so it derefs. fake with an atom.
        error-fn (atom (fn [& _]
                         (abort
                          (str task " is not a task. Use \"help\" "
                               "to list all tasks."))))]
    (try
     (when-not (find-ns task-ns)
       (require task-ns))
     (or (ns-resolve task-ns task)
         error-fn)
     (catch java.io.FileNotFoundException e
       error-fn))))

(defn- hookize [v]
  (when-not (::hooks (meta @v))
    (alter-var-root v vary-meta assoc ::hooks (atom []))))

(defn add-hook [task-var f]
  (hookize task-var)
  (swap! (::hooks (meta @task-var)) conj f))

(defn- load-hooks [task]
  (doseq [n (sort (find-namespaces-on-classpath))
          :when (re-find #"^leiningen\.hooks\." (name n))]
    (require n)))

;; These two were taken from fixtures in clojure.test; thanks Stuart!
(defn- compose-hooks [f1 f2]
  (fn [g] (f1 (fn [] (f2 g)))))

(defn- join-hooks [hooks]
  (reduce compose-hooks #(%) (and hooks @hooks)))

(defn run-task
  "Run the given task with its hooks activated."
  [task-name args]
  (let [task (resolve-task task-name)]
    (load-hooks task-name)
    ((join-hooks (::hooks (meta @task)))
     #(apply @task args))))

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
  ([& [task & args]]
     (let [task (or (@aliases task) task "help")
           args (if (@no-project-needed task)
                  args
                  (conj args (read-project)))
           compile-path (:compile-path (first args))]
       (when compile-path (.mkdirs (File. compile-path)))
       (binding [*compile-path* compile-path]
         ;; TODO: can we catch only task-level arity problems here?
         ;; compare args and (:arglists (meta (resolve-task task)))?
         (let [value (run-task task args)]
           (when (integer? value)
             (System/exit value))))))
  ([] (apply -main (or *command-line-args* ["help"]))))
