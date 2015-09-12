(ns leiningen.new.template
  (:require [leiningen.new.templates :refer [renderer sanitize year date ->files]]
            [leiningen.core.main :as main]))

(defn template
  "A meta-template for 'lein new' templates."
  [name]
  (let [render (renderer "template")
        data {:name name
              :sanitized (sanitize name)
              :placeholder "{{sanitized}}"
              :year (year)
              :date (date)}]
    (main/info "Generating fresh 'lein new' template project.")
    (->files data
             ["README.md" (render "README.md" data)]
             ["project.clj" (render "project.clj" data)]
             [".gitignore" (render "gitignore" data)]
             [".hgignore" (render "hgignore" data)]
             ["src/leiningen/new/{{sanitized}}.clj" (render "temp.clj" data)]
             ["resources/leiningen/new/{{sanitized}}/foo.clj" (render "foo.clj")]
             ["LICENSE" (render "LICENSE" data)]
             ["CHANGELOG.md" (render "CHANGELOG.md" data)])))
