(ns leiningen.new.{{name}}
    (:use leiningen.new.templates))

(def render (renderer "{{name}}"))

(defn {{name}}
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (sanitize name)}]
    (->files data
             ["src/{{placeholder}}/foo.clj" (render "foo.clj" data)])))
