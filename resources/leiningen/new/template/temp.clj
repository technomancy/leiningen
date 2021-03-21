(ns leiningen.new.{{group-id}}.{{artifact-id}}
  (:require [leiningen.new.templates :as tmpl]
            [leiningen.core.main :as main]))

(def render (tmpl/renderer "{{sanitized}}"))

(defn {{artifact-id}}
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (tmpl/name-to-path name)}]
    (main/info "Generating fresh 'lein new' {{name}} project.")
    (tmpl/->files data
                  ["src/{{placeholder}}/foo.clj" (render "foo.clj" data)])))
