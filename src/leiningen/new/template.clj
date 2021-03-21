(ns leiningen.new.template
  (:require [clojure.string :as str]
            [leiningen.new.templates :as t]
            [leiningen.core.main :as main]))

(defn template
  "A meta-template for 'lein new' templates."
  [template-name]
  (when-not (namespace (symbol template-name))
    (main/warn (str "Template names must use a group-id to conform with new"
                    " Clojars security policy:\n"
                    "https://github.com/clojars/clojars-web/wiki/Verified-Group-Names"
                    "\n\nYou may generate this template but you may not be"
                    " able to publish it on Clojars.")))
  (let [render (t/renderer "template")
        sym (symbol template-name)
        data {:name template-name
              :artifact-id (name sym)
              :group-id (namespace sym)
              :sanitized (t/name-to-path template-name)
              :placeholder "{{sanitized}}"
              :year (t/year)
              :date (t/date)}]
    (main/info "Generating fresh 'lein new' template project.")
    (t/->files data
               ["README.md" (render "README.md" data)]
               ["project.clj" (render "project.clj" data)]
               [".gitignore" (render "gitignore" data)]
               [".hgignore" (render "hgignore" data)]
               ["src/leiningen/new/{{sanitized}}.clj" (render "temp.clj" data)]
               ["resources/leiningen/new/{{sanitized}}/foo.clj" (render "foo.clj")]
               ["LICENSE" (render "LICENSE" data)]
               ["CHANGELOG.md" (render "CHANGELOG.md" data)])))
