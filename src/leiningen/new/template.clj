(ns leiningen.new.template
  (:use leiningen.new.templates))

(def render (renderer "template"))

(defn template
  "A skeleton 'lein new' template."
  [name]
  (let [data {:name name
              :sanitized (sanitize name)
              :placeholder "{{sanitized}}"}]
    (println "Generating skeleton 'lein new' template project.")
    (->files data
             ["README.md" (render "README.md" data)]
             ["project.clj" (render "project.clj" data)]
             [".gitignore" (render "gitignore" data)]
             ["src/leiningen/new/{{sanitized}}.clj" (render "temp.clj" data)]
             ["src/leiningen/new/{{sanitized}}/foo.clj" (render "foo.clj")])))
