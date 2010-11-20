(ns leiningen.run
  (:use [leiningen.compile :only [eval-in-project]]
        [leiningen.core :only [abort]]))

(defn- run-main
  "Loads the project namespaces as well as all its dependencies and then calls
  ns/f, passing it the args."
  ([project ns & args]
     (eval-in-project project `((ns-resolve '~(symbol ns) '~'-main) ~@args)
                      nil nil `(require '~(symbol ns)))))

;; TODO: use subtask help?
(defn run
  "Call a -main function with optional command-line arguments.

USAGE: lein run [ARGS...]
Calls the -main function in the namespace specified as :main in project.clj.

USAGE: lein run -m NAMESPACE [ARGS...]
Calls the -main function in the specified namespace.

USAGE: lein run :alias [ARGS...]
Aliases can be defined in project.clj as
    :run-aliases {:alias a.namespace
                  :alias2 another.namespace}"
  [project & [flag & args]]
  (let [flag (and flag (read-string flag))
        alias (and (keyword? flag) (flag (:run-aliases project)))]
    (cond alias           (apply run project "-m" (cons alias args))
          (= flag '-m)    (apply run-main project args)
          (:main project) (apply run-main project (:main project) flag args)
          :else (abort "No :main namespace specified in project.clj."))))
