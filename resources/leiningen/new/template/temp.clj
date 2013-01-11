(ns leiningen.new.{{name}}
  (:use [leiningen.new.templates :only [renderer name-to-path ->files]]))

(def render (renderer "{{name}}"))

(defn {{name}}
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}]
    (->files data
             ["src/{{placeholder}}/foo.clj" (render "foo.clj" data)])))
