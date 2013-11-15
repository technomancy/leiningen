(ns leiningen.new
  "Generate project scaffolding based on a template."
  (:refer-clojure :exclude [new list])
  (:require [bultitude.core :as bultitude]
            [leiningen.core.main :refer [abort]]
            [leiningen.new.templates :refer [*dir*]])
  (:import java.io.FileNotFoundException))

(defn- fake-project [name]
  {:templates [[(symbol name "lein-template") "RELEASE"]]
   :repositories {"clojars" {:url "http://clojars.org/repo/"
                             :update :always}
                  "central" {:url "http://repo1.maven.org/maven2"
                             :update :always}}})

(defn resolve-remote-template [name sym]
  (if-let [get-dep (resolve 'leiningen.core.classpath/resolve-dependencies)]
    (try (get-dep :templates (fake-project name) :add-classpath? true)
         (require sym)
         true
         (catch Exception _))))

(defn resolve-template [name]
  (let [sym (symbol (str "leiningen.new." name))]
    (if (try (require sym)
             true
             (catch FileNotFoundException _
               (resolve-remote-template name sym)))
      (resolve (symbol (str sym "/" name)))
      (abort "Could not find template" name "on the classpath."))))

;; A lein-newnew template is actually just a function that generates files and
;; directories. We have a bit of convention: we expect that each template is on
;; the classpath and is based in a .clj file at `leiningen/new/`. Making this
;; assumption, a user can simply give us the name of the template he wishes to
;; use and we can `require` it without searching the classpath for it or doing
;; other time consuming things. If this namespace isn't found and we are
;; running Leiningen 2, we can resolve it via pomegranate first.
;;
;; Since our templates are just function calls just like Leiningen tasks, we can
;; also expect that a template generation function also be named the same as the
;; last segment of its namespace. This is what we call to generate the project.
(defn create
  [template name & args]
  (cond
   (and (re-find #"(?i)(?<!(clo|compo))jure" name)
        (not (System/getenv "LEIN_IRONIC_JURE")))
   (abort "Sorry, names such as clojure or *jure are not allowed."
          "\nIf you intend to use this name ironically, please set the"
          "\nLEIN_IRONIC_JURE environment variable and try again.")
   (= name "clojure")
   (abort "Sorry, clojure can't be used as a project name."
          "\nIt will confuse Clojure compiler and cause obscure issues.")
   (and (re-find #"[A-Z]" name)
        (not (System/getenv "LEIN_BREAK_CONVENTION")))
   (abort "Project names containing uppercase letters are not recommended"
          "\nand will be rejected by repositories like Clojars and Central."
          "\nIf you're truly unable to use a lowercase name, please set the"
          "\nLEIN_BREAK_CONVENTION environment variable and try again.")
   (not (symbol? (try (read-string name) (catch Exception _))))
   (abort "Project names must be valid Clojure symbols.")
   :else (apply (resolve-template template) name args)))

;; Since we have our convention of templates always being at
;; `leiningen.new.<template>`, we can easily search the classpath
;; to find templates in the same way that Leiningen can search to
;; find tasks. Furthermore, since our templates will always have a
;; function named after the template that is the entry-point, we can
;; also expect that it has the documentation for the template. We can
;; just look up these templates on the classpath, require them, and then
;; get the metadata off of that function to list the names and docs
;; for all of the available templates.

(defn list []
  (for [n (bultitude/namespaces-on-classpath :prefix "leiningen.new.")
        ;; There are things on the classpath at `leiningen.new` that we
        ;; don't care about here. We could use a regex here, but meh.
        :when (not= n 'leiningen.new.templates)]
    (-> (doto n require)
        (the-ns)
        (ns-resolve (symbol (last (.split (str n) "\\.")))))))

(defn show
  "Show details for a given template."
  [name]
  (let [resolved (meta (resolve-template name))]
    (println (:doc resolved "No documentation available."))
    (println)
    (println "Argument list:" (or (:help-arglists resolved)
                                  (:arglists resolved)))))

(def ^{:dynamic true :doc "Bound to project map at runtime"} *project* nil)

(defn is-option? [str]
  (.startsWith str "-"))

(defn parse-options
  "Given a sequence of strings, return a map of command-line-esque
  options with keyword-ized keys and a list of additional args:

  (parse-options [\"--chicken\"])
  => [{:--chicken true} []]

  (parse-options [\"--beef\" \"rare\"])
  => [{:--beef rare} []]

  (parse-options [\"salmon\" \"trout\"])
  => [{} [\"salmon\" \"trout\"]]

  (parse-options [\"--to-dir\" \"test2\" \"--ham\" \"-bacon\"])
  => [{:-bacon true, :--ham true, :--to-dir \"test2\"} []]

  (parse-options [\"--to-dir\" \"test2\" \"--ham\" \"-bacon\" \"--\" \"pate\"])
  => [{:-bacon true, :--ham true, :--to-dir \"test2\"} [\"pate\"]]
"
  [options]
  (loop [m {}
         [first-arg second-arg & rest :as args] options]
    (if (and first-arg (is-option? first-arg) (not= "--" first-arg))
      (if (or (not second-arg) (is-option? second-arg))
        (recur (assoc m (keyword first-arg) true) (if second-arg (cons second-arg rest) rest))
        (recur (assoc m (keyword first-arg) second-arg) rest))
      [m (if (= "--" first-arg)
           (if second-arg (cons second-arg rest) [])
           (or args []))])))

(defn- project-name-specified? [[first-arg & _]]
  (and first-arg (not (is-option? first-arg))))

(defn- template-specified? [[_ second-arg & _]]
  (and second-arg (not (is-option? second-arg))))

(defn- parse-args [[first-arg second-arg & opts :as args]]
  (if (project-name-specified? args)
    (let [template-name (if (template-specified? args) first-arg nil)
          new-project-name (if (template-specified? args) second-arg first-arg)
          options (parse-options (if (template-specified? args)
                                   opts
                                   (if second-arg (cons second-arg opts) opts)))]
      [template-name new-project-name options])
    [nil nil (parse-options args)]))

(defn- print-help []
  (require 'leiningen.help)
  ((ns-resolve 'leiningen.help 'help) nil "new"))

(defn ^{:no-project-needed true
        :help-arglists '[[project project-name]
                         [project template project-name & args]]
        :subtasks (list)}
  new
  "Generate scaffolding for a new project based on a template.

If only one argument is passed to the \"new\" task, the default template
is used and the argument is used as the name of the project.

If two arguments are passed, the first should be the name of a template,
and the second is used as the name of the project, for example:

    lein new $TEMPLATE_NAME $PROJECT_NAME

To generate to a directory different than your project's name use --to-dir:

    lein new $TEMPLATE_NAME $PROJECT_NAME --to-dir $DIR

Arguments can be passed to templates by adding them after \"new\"'s options:

    lein new $TEMPLATE_NAME $PROJECT_NAME --to-dir $DIR template-arg-1 template-arg-2

The list of built-in templates can be shown with `lein help new`. Third-party
templates can be found at https://clojars.org/search?q=lein-template.
When creating a new project from a third-party template, use its group-id
as the template name. Note that there's no need to \"install\" a given third-
party template --- lein will automatically fetch it for you.

Use `lein new $TEMPLATE :show` to see details about a given template.

To create a new template of your own, see the documentation for the
lein-new Leiningen plug-in."

  [project & args]
  (binding [*project* project]
    (let [[template-name new-project-name [options template-args]] (parse-args args)]
      (if (or (not new-project-name) (:--help options))
        (print-help)
        (if (and template-name (= ":show" new-project-name))
          (show template-name)
          (binding [*dir* (:--to-dir options)]
            (apply create (or template-name "default") new-project-name template-args)))))))
