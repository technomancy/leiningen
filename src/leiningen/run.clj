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

(defn- prep-arg
  "Prepares an argument with the given preparation method."
  [prep-method arg]
  (case prep-method
    :stringify (if (string? arg) arg
                   (pr-str arg)) ;; print-dup/print-meta seems overkill
    :quote (list 'quote arg)
    (throw (ex-info "Unknown preparation method"
                    {:prep-method prep-method}))))

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
                    [:var v#]
                    [:not-found]))
                (catch FileNotFoundException e#
                  [:threw e#]))

           ;; If we didn't succeed above, check if a class exists for
           ;; the given name
           ^Class class#
           (when-not (= :var ns-flag#)
             (try (Class/forName ~(name given))
                  (catch ClassNotFoundException _#)))]
       (cond
         (= :var ns-flag#)
         (data# ~@args)

         ;; If the class exists, run its main method.
         class#
         ;; NOTE: this prints a reflection warning, but it is not trivially solvable because
         ;;       metadata in the argument is not preserved when writing forms to be eval'ed.
         ;;       See https://github.com/technomancy/leiningen/issues/2328 and
         ;        https://github.com/technomancy/leiningen/issues/2814
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

(defn- parse-args
  "Parses the arguments passed in to run, and returns a map with the results. If
  an argument is given more than once, it is assumed to be an argument to the
  main function."
  ([args] (parse-args args {}))
  ([[f & rest-args :as args] cur-args]
   (if-not (seq args)
     (merge {:arg-conversion :stringify
             :args args} cur-args)
     (cond (= "--" f)
           (merge {:arg-conversion :stringify
                   :args rest-args}
                  cur-args)
           (and (or (= "-m" f)
                    (= ":main" f))
                (not (contains? cur-args :main)))
           (if (first rest-args)
             (->> (assoc cur-args :main (first rest-args))
                  (parse-args (rest rest-args)))
             (main/abort "Option -m requires a namespace argument."))
           (and (= "--quote-args" f)
                (not (contains? cur-args :arg-conversion)))
           (->> (assoc cur-args :arg-conversion :quote)
                (parse-args rest-args))
           :else
           (merge {:arg-conversion :stringify
                   :args args}
                  cur-args)))))

(defn- run-main
  "Loads the project namespaces as well as all its dependencies and then calls
  ns/f, passing it the args."
  [project given prep-type args]
  ;; must convert lazy-seq to list(issue #2091)
  ;; eval can't handle well a form that contains an evaluated empty lazy-seq
  (let [prepped-args (apply list (map #(prep-arg prep-type %) args))]
    (try (eval/eval-in-project project (run-form given prepped-args))
         (catch clojure.lang.ExceptionInfo e
           (main/exit (:exit-code (ex-data e) 1))))))

(defn ^{:help-arglists '([]) :no-project-needed true} run
  "Run the project's -main function.

USAGE: lein run [--] [ARGS...]
Calls the -main function in the namespace specified as :main in project.clj.
You may use -- to escape the first argument in case it begins with `-' or `:'.
If your main function is not called \"-main\", you may use a namespaced symbol
like clojure.main/main.

USAGE: lein run -m NAMESPACE[/MAIN_FUNCTION] [--] [ARGS...]
Calls the main function in the specified namespace. You may have to use -- to
escape the first argument in case it begins with `-' or `:'.

The `--quote-args' flag quotes the arguments passed in, instead of converting
them to strings. Arguments coming from the command line will always be strings,
so this is only useful when invoked from :aliases.

See also \"lein help trampoline\" for a way to save memory using this task."
  [project & raw-args]
  (let [arg-map (parse-args raw-args)]
    (if (and (not (:main arg-map))
             (not (:main project)))
      (main/abort "No :main namespace specified in project.clj.")
      (run-main project (or (:main arg-map) (:main project))
                (:arg-conversion arg-map) (:args arg-map)))))
