(ns leiningen.run
  "Run a -main function with optional command-line arguments."
  (:use [leiningen.compile :only [eval-in-project]]
        [leiningen.core :only [abort]]))

(defn- get-ns-and-fn [given]
  (if (= 'clojure.main given) ; special-case this oddity
    ['clojure.main 'main]
    (let [[given-ns given-sym] ((juxt namespace name) given)]
      (map symbol (if given-ns
                    [given-ns given-sym]
                    [given-sym "-main"])))))

(defn- run-main
  "Loads the project namespaces as well as all its dependencies and then calls
  ns/f, passing it the args."
  ([project given-main & args]
     (let [[main-ns main-fn] (get-ns-and-fn (symbol given-main))]
          (eval-in-project project `((ns-resolve '~main-ns '~main-fn) ~@args)
                           nil nil `(require '~main-ns)))))

(defn ^{:help-arglists '([])} run
  "Run the project's -main function.

USAGE: lein run [--] [ARGS...]
Calls the -main function in the namespace specified as :main in project.clj.
You may use -- to escape the first argument in case it begins with `-' or `:'.
If your main function is not called \"-main\", you may use a namespaced symbol
like clojure.main/main.

USAGE: lein run -m NAMESPACE[/MAIN_FUNCTION] [ARGS...]
Calls the main function in the specified namespace.

USAGE: lein run :alias [ARGS...]
Aliases can be defined in project.clj as
    :run-aliases {:alias a.namespace/my-main
                  :alias2 another.namespace}

See also \"lein help trampoline\" for a way to save memory using this task."
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
