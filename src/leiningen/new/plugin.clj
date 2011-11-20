(ns leiningen.new.plugin
  (:use leiningen.new.templates))

(def render (renderer "plugin"))

(defn plugin
  "A leiningen plugin project."
  [name]
  (let [unprefixed (if (.startsWith name "lein-")
                     (subs name 5)
                     name)
        data {:name name
              :unprefixed-name unprefixed
              :sanitized (sanitize unprefixed)}]
    (println (str "Generating a skeleton Leiningen plugin called " name "."))
    (->files data
             ["project.clj" (render "project.clj" data)]
             ["README.md" (render "README.md" data)]
             [".gitignore" (render "gitignore" data)]
             ["src/leiningen/{{sanitized}}.clj" (render "name.clj" data)])))
