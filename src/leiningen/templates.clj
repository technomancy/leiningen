(ns leiningen.templates
  "List templates on the classpath."
  (:use [leiningen.core.ns :only [namespaces-matching]]))

;; Since we have our convention of templates always being at
;; `leiningen.new.<template>`, we can easily search the classpath
;; to find templates in the same way that Leiningen can search to
;; find tasks. Furthermore, since our templates will always have a
;; function named after the template that is the entry-point, we can
;; also expect that it has the documentation for the template. We can
;; just look up these templates on the classpath, require them, and then
;; get the metadata off of that function to list the names and docs
;; for all of the available templates.
(defn ^{:no-project-needed true}
  templates
  "List available 'lein new' templates"
  [project]
  (println "List of 'lein new' templates on the classpath:")
  ;; There are things on the classpath at `leiningen.new` that we
  ;; don't care about here. We could use a regex here, but meh.
  (doseq [n (remove '#{leiningen.new.templates leiningen.new}
                    (namespaces-matching "leiningen.new"))]
    (require n)
    (let [n-meta (meta
                  (ns-resolve (the-ns n)
                              (symbol (last (.split (str n) "\\.")))))]
      (println (str (:name n-meta) ":")
               (or (:doc n-meta) "No documentation available.")))))