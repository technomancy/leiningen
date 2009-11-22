(ns leiningen.core
  (:use [clojure.contrib.with-ns])
  (:gen-class))

(def project nil)

(defmacro defproject [project-name version & args]
  ;; This is necessary since we must allow defproject to be eval'd in
  ;; any namespace due to load-file; we can't just create a var with
  ;; def or we would not have access to it once load-file returned.
  `(do (alter-var-root #'project
                       (fn [_#] (assoc (apply hash-map (quote ~args))
                                  :name ~(name project-name)
                                  :group ~(or (namespace project-name)
                                              (name project-name))
                                  :version ~version
                                  :root ~(.getParent (java.io.File. *file*)))))
       (def ~(symbol (name project-name)) project)))

;; So it doesn't need to be fully-qualified in project.clj
(with-ns 'clojure.core (use ['leiningen.core :only ['defproject]]))

(defn read-project
  ([file] (load-file file)
     project)
  ([] (read-project "project.clj")))

(def aliases {"--help" "help" "-h" "help" "-?" "help"})

(defn command-not-found [command project & _]
  (println command "is not a command. Use \"help\" to list all commands.")
  (System/exit 1))

(defn resolve-command [command]
  (let [command-ns (symbol (str "leiningen." command))
        command (symbol command)]
    (try
     (require command-ns)
     (ns-resolve command-ns command)
     (catch java.io.FileNotFoundException e
       (partial command-not-found command)))))

(defn -main [command & args]
  (let [command (or (aliases command) command)
        project (if (= command "new") ; only new works without a project.clj
                  (first args)
                  (read-project))]
    (binding [*compile-path* (or (:compile-path project)
                                 (str (:root project) "/classes/"))]
      (apply (resolve-command command) project args))
    ;; In case tests or some other task started any:
    (shutdown-agents)))
