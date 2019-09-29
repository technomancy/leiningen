(ns leiningen.new. {{name}}
    (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
              [leiningen.core.main :as main]))

(def render (renderer "{{name}}"))

(defn {{name}}
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}]
    (main/info "Generating fresh 'lein new' {{name}} project.")
    (->files data
             ["src/{{placeholder}}/foo.clj" (render "foo.clj" data)])))
