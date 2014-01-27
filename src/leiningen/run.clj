(ns leiningen.run
  "Run a -main function with optional command-line arguments."
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main])
  (:import (java.io FileNotFoundException)
           (clojure.lang Reflector)))

(defn- normalize-main [given]
  (when-not (or (symbol? given)
                (and (string? given) (symbol? (read-string given))))
    (main/abort (str "Option -m requires a valid namespace argument, not "
                     given ".")))
  (if (namespace (symbol given))
    (symbol given)
    (symbol (name given) "-main")))

;; TODO: get rid of this in 3.0
(defn run-form
  "Construct a form to run the given main defn or class with arguments."
  [given args]
  `(binding [*command-line-args* '~args]
     ;;
     ;; Some complicated error-handling logic here to support main
     ;; being either a namespace or a class.
     ;;
     ;; The use case that prompted this complexity is that if we
     ;; have a namespace such as:
     ;;
     ;;   (ns foo.main
     ;;     (:require does.not.exist))
     ;;
     ;; and we do a `lein run -m foo.main`, we will get a
     ;; FileNotFoundException, but NOT because foo.main doesn't
     ;; exist. So we want to make sure that error propogates
     ;; up in the event that the class doesn't exist as well.
     ;; But we still have to try the class first because it's
     ;; not easy to distinguish the above case from the case
     ;; when foo.main is a class and not a namespace at all.
     ;;
     ;; This would be a lot simpler if we weren't trying to
     ;; transparently support both namespaces and classes specified in
     ;; the same way.
     ;;

     ;; Try to require the namespace and run the appropriate var,
     ;; noting what happened.
     (let [[ns-flag# data#]
           (try (require '~(symbol (namespace
                                    (normalize-main given))))
                (let [v# (resolve '~(normalize-main given))]
                  (if (ifn? v#)
                    [:done (v# ~@args)]
                    [:not-found]))
                (catch FileNotFoundException e#
                  [:threw e#]))

           ;; If we didn't succeed above, check if a class exists for
           ;; the given name
           class#
           (when-not (= :done ns-flag#)
             (try (Class/forName ~(name given))
                  (catch ClassNotFoundException _#)))]
       (cond
        (= :done ns-flag#) data#

        ;; If the class exists, run its main method.
        class#
        (Reflector/invokeStaticMethod
         class# "main" (into-array [(into-array String '~args)]))

        ;; If the symbol didn't resolve, give a reasonable message
        (= :not-found ns-flag#)
        (throw (Exception. ~(str "Cannot find anything to run for: " (name given))))

        ;; If we got an exception earlier and nothing else worked,
        ;; rethrow that.
        (= :threw ns-flag#)
        (do (binding [*out* *err*]
              (println (str "Can't find '" '~given "' as .class or .clj for "
                            "lein run: please check the spelling.")))
            (throw data#))))))

(defn- run-main
  "Loads the project namespaces as well as all its dependencies and then calls
  ns/f, passing it the args."
  [project given & args]
  (try (eval/eval-in-project project (run-form given args))
       (catch clojure.lang.ExceptionInfo e
         (main/abort))))

(defn ^{:help-arglists '([]) :no-project-needed true} run
  "Run the project's -main function.

USAGE: lein run [--] [ARGS...]
Calls the -main function in the namespace specified as :main in project.clj.
You may use -- to escape the first argument in case it begins with `-' or `:'.
If your main function is not called \"-main\", you may use a namespaced symbol
like clojure.main/main.

USAGE: lein run -m NAMESPACE[/MAIN_FUNCTION] [ARGS...]
Calls the main function in the specified namespace.

See also \"lein help trampoline\" for a way to save memory using this task."
  [project & [flag & args :as all-args]]
  (let [all-args (if (= flag "--") args all-args)]
    (cond (or (= flag ":main")
              (= flag "-m")) (if (first args)
                               (apply run-main project args)
                               (main/abort "Option -m requires a namespace argument."))
              (:main project) (apply run-main project (:main project) all-args)
              :else (main/abort "No :main namespace specified in project.clj."))))
