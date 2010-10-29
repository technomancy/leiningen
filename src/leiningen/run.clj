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
  "Call a function in a new process or run a .clj file.

USAGE: lein run [ARGS...]
Calls the -main function in the namespace specified as :main in project.clj.

USAGE: lein run -m NAMESPACE [ARGS...]
Calls the namespace/function in a new process; function defaults to -main.

USAGE: lein run :alias [ARGS...]
Aliases can be defined in project.clj as
    :run-aliases {:alias a.namespace
                  :alias2 another.namespace}"
  [project & [first-arg & args]]
  (let [first-arg (read-string first-arg)
        alias (and (keyword? first-arg) (first-arg (:run-aliases project)))]
    (cond alias              (apply run project "-m" (cons alias args))
          (= first-arg '-m) (apply run-main project args)
          (:main project)    (apply run-main project (:main project)
                                    first-arg args)
          :else (abort "No :main namespace specified in project.clj."))))
