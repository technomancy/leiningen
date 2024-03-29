(ns leiningen.new
  "Generate project scaffolding based on a template."
  (:refer-clojure :exclude [new])
  (:require [clojure.string :as str]
            [leiningen.core.classpath :as cp]
            [leiningen.core.project :as project]
            [leiningen.core.user :as user]
            [leiningen.core.main :refer [abort parse-options option-arg debug]]
            [leiningen.new.templates :refer [*dir* *force?*]]
            leiningen.new.template
            leiningen.new.plugin
            leiningen.new.default
            leiningen.new.app)
  (:import java.io.FileNotFoundException))

(def ^:dynamic *use-snapshots?* false)
(def ^:dynamic *template-version* nil)

(defn- template-symbol
  "The old style of template artifacts was $FOO/lein-template where
  the short name of the template was used as the group-id within
  Clojars, but Clojars will not allow new templates in this style to
  be created going forward. The new style is $GROUP/lein-template.$ARTIFACT
  so `lein new us.technomancy/my-stuff would look up
  us.technomancy/lein-template.my-stuff in the remote repository."
  [template-name]
  (if (re-find #"/" template-name)
    (let [[group-id artifact-id] ((juxt namespace name) (symbol template-name))]
      (symbol group-id (str "lein-template." artifact-id)))
    (symbol template-name "lein-template")))

(defn- fake-project [name]
  (let [template-version (cond *template-version* *template-version*
                               *use-snapshots?*   "(0.0.0,)"
                               :else              "RELEASE")
        user-profiles (:user (user/profiles))
        repositories (reduce
                       (:reduce (meta project/default-repositories))
                       project/default-repositories
                       (:plugin-repositories user-profiles))]
    (merge {:templates [[(template-symbol name) template-version]]
            :repositories repositories}
           (select-keys user-profiles [:mirrors]))))

(defn resolve-remote-template [name ns-sym]
  (try (cp/resolve-dependencies :templates (fake-project name)
                                :add-classpath? true)
       (require ns-sym)
       true
       (catch clojure.lang.Compiler$CompilerException e
         (abort (str "Could not load template, failed with: " (.getMessage e))))
       (catch Exception e
         (debug (str e)))))

(defn resolve-template [template-name]
  (let [ns-sym (symbol (str "leiningen.new." (name (symbol template-name))))]
    (if (try (require (symbol (str "leiningen.new." template-name)))
             true
             (catch FileNotFoundException _
               (resolve-remote-template template-name ns-sym)))
      (resolve (symbol (name ns-sym) (name (symbol template-name))))
      (abort "Could not find template for" template-name
             "on the classpath: " ns-sym))))

;; A lein-newnew template is actually just a function that generates files and
;; directories. We have a bit of convention: we expect that each template is on
;; the classpath and is based in a .clj file at `leiningen/new/`. Making this
;; assumption, users can simply give us the name of the template they wish to
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
   (and (re-find #"(?i)(?<!(cl|comp))eauxure" name)
        (not (System/getenv "LEIN_IRONIC_EAUXURE")))
   (abort "Sorry, names such as cleauxure or *eauxure are not allowed."
          "\nIf you intend to use this name ironically, please set the"
          "\nLEIN_IRONIC_EAUXURE environment variable and try again.")
   (or (= name "clojure") (= name "cljs"))
   (abort "Sorry, clojure and cljs can't be used as project names."
          "\nIt will confuse the compiler and cause obscure issues.")
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

(defn show
  "Show details for a given template."
  [name]
  (let [resolved (meta (resolve-template name))]
    (println (:doc resolved "No documentation available."))
    (println)
    (println "Argument list:" (or (:help-arglists resolved)
                                  (:arglists resolved)))))

(def ^{:dynamic true :doc "Bound to project map at runtime"} *project* nil)

(defn- project-name-specified? [[first-arg & _]]
  (and first-arg (not (option-arg first-arg))))

(defn- template-specified? [[_ second-arg & _]]
  (and second-arg (not (option-arg second-arg))))

(defn- parse-args [[first-arg second-arg & opts :as args]]
  (if (project-name-specified? args)
    (let [template-name (if (template-specified? args) first-arg nil)
          new-project-name (if (template-specified? args) second-arg first-arg)
          options (parse-options (if (template-specified? args)
                                   opts
                                   (if second-arg
                                     (cons second-arg opts) opts)))]
      [template-name new-project-name options])
    [nil nil (parse-options args)]))

(defn- print-help []
  (require 'leiningen.help)
  ((ns-resolve 'leiningen.help 'help) nil "new"))

(defn ^{:no-project-needed true
        :help-arglists '[[project project-name]
                         [project template project-name [-- & args]]]
        :subtasks [#'leiningen.new.template/template
                   #'leiningen.new.plugin/plugin
                   #'leiningen.new.default/default
                   #'leiningen.new.app/app]}
  new
  "Generate scaffolding for a new project based on a template.

If only one argument is passed to the \"new\" task, the default template
is used and the argument is used as the name of the project.

If two arguments are passed, the first should be the name of a template,
and the second is used as the name of the project, for example:

    lein new $TEMPLATE_NAME $PROJECT_NAME

To generate to a directory different than your project's name use --to-dir:

    lein new $TEMPLATE_NAME $PROJECT_NAME --to-dir $DIR

By default, the \"new\" task will not write to an existing directory.
Supply the --force option to override this behavior:

    lein new $TEMPLATE_NAME $PROJECT_NAME --force
    lein new $TEMPLATE_NAME $PROJECT_NAME --to-dir $DIR --force

Arguments can be passed to templates by adding them after \"new\"'s options. Use
`--` to separate arguments to lein new and the actual template you are using:

    lein new $TEMPLATE_NAME $PROJECT_NAME --to-dir $DIR -- template-arg-1 template-arg-2

If you'd like to use an unreleased (ie, SNAPSHOT) template, pass in --snapshot:

    lein new $TEMPLATE_NAME $PROJECT_NAME --snapshot

If you'd rather like to use a specific version of template, specify the version
with --template-version option:

    lein new $TEMPLATE_NAME $PROJECT_NAME --template-version $TEMPLATE_VERSION

If you use the `--snapshot` or `--template-version` argument with template args
you may need to use `--` to prevent template args from being interpreted as
arguments to `lein new`:

    lein new $TEMPLATE_NAME $PROJECT_NAME --snapshot -- template-arg-1 template-arg-2

Third-party templates can be found at by searching on Clojars:
  https://clojars.org/search?q=artifact-id:lein-template*

Note that there's no need to \"install\" a given third- party template; lein
will automatically fetch it for you.

Use `lein new :show $TEMPLATE` to see details about a given template.

To create a new template of your own, run `lein help templates`."
  [project & args]
  (binding [*project* project]
    (let [[template-name new-project-name
           [options template-args]] (parse-args args)]
      (if (or (:--help options) (empty? args))
        (print-help)
        (if-let [show-template (or (and (true? (:show options))
                                        new-project-name)
                                   (:show options) (:--show options))]
          (show show-template)
          (binding [*dir* (or (:to-dir options) (:--to-dir options))
                    *use-snapshots?* (or (:snapshot options)
                                         (:--snapshot options))
                    *template-version* (or (:template-version options)
                                           (:--template-version options))
                    *force?* (or (:force options) (:--force options))]
            (apply create (or template-name "default")
                   new-project-name template-args)))))))
