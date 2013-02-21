(ns leiningen.new.template
  (:use [leiningen.new.templates :only [renderer sanitize year ->files]]))

(defn template
  "A meta-template for 'lein new' templates."
  [name]
  (let [render (renderer "template")
        data {:name name
              :sanitized (sanitize name)
              :placeholder "{{sanitized}}"
              :year (year)}]
    (println "Generating fresh 'lein new' template project.")
    (->files data
             ["README.md" (render "README.md" data)]
             ["project.clj" (render "project.clj" data)]
             [".gitignore" (render "gitignore" data)]
             ["src/leiningen/new/{{sanitized}}.clj" (render "temp.clj" data)]
             ["src/leiningen/new/{{sanitized}}/foo.clj" (render "foo.clj")])))
