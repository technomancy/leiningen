(ns leiningen.new
  "Generate project scaffolding based on a template."
  (:import java.io.FileNotFoundException))

;; A leiningen.new template is actually just a function that generates files and
;; directories. We have a bit of convention: we expect that each template is on
;; the classpath and is based in a .clj file at `leiningen/new/`. Making this
;; assumption, a user can simply give us the name of the template he wishes to
;; use and we can `require` it without searching the classpath for it or doing
;; other time consuming things.
;;
;; Since our templates are just function calls just like Leiningen tasks, we can
;; also expect that a template generation function also be named the same as the
;; last segment of its namespace. This is what we call to generate the project.
;; If the template's namespace is not on the classpath, we can just catch the
;; FileNotFoundException and print a nice safe message.
(defn ^{:no-project-needed true}
  new
  "Generate scaffolding for a new project based on a template.

If only one argument is passed, the default template is used and the
argument is treated as if it were the name of the project."
  ([project project-name] (leiningen.new/new project "default" project-name))
  ([project template & args]
     (let [sym (symbol (str "leiningen.new." template))]
       (if (try (require sym)
                (catch FileNotFoundException _ true))
         (println "Could not find template" template "on the classpath.")
         (apply (resolve (symbol (str sym "/" template))) args)))))