(ns leiningen.run
  "Run a -main function with optional command-line arguments."
  (:use [leiningen.compile :only [eval-in-project]]
        [leiningen.core :only [abort]]))

(defn- run-main
  "Loads the project namespaces as well as all its dependencies and then calls
  ns/f, passing it the args."
  ([project ns & args]
     (eval-in-project project `((ns-resolve '~(symbol ns) '~'-main) ~@args)
                      nil nil `(require '~(symbol ns)))))

(defn ^{:help-arglists '([])} run
  "Run a -main function with optional command-line arguments.

USAGE: lein run [--] [ARGS...]
Calls the -main function in the namespace specified as :main in project.clj.
You may use -- to escape the first argument in case it begins with `-' or `:'.

USAGE: lein run -m NAMESPACE [ARGS...]
Calls the -main function in the specified namespace.

USAGE: lein run :alias [ARGS...]
Aliases can be defined in project.clj as
    :run-aliases {:alias a.namespace
                  :alias2 another.namespace}"
  [project & [flag & args :as all-args]]
  (let [kw (when (= (first flag) \:) (keyword (subs flag 1)))
        alias (get (:run-aliases project) kw)
        all-args (if (= flag "--") args all-args)]
    (cond alias           (apply run project "-m" (cons alias args))
          (= flag "-m")   (if (first args)
                            (apply run-main project args)
                            (abort "Option -m requires a namespace argument."))
          (:main project) (apply run-main project (:main project) all-args)
          :else (abort "No :main namespace specified in project.clj."))))
