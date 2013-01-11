(ns leiningen.new.plugin
  (:use [leiningen.new.templates :only [renderer sanitize year ->files]]))

(defn plugin
  "A leiningen plugin project template."
  [name]
  (let [render (renderer "plugin")
        unprefixed (if (.startsWith name "lein-")
                     (subs name 5)
                     name)
        data {:name name
              :unprefixed-name unprefixed
              :sanitized (sanitize unprefixed)
              :year (year)}]
    (println (str "Generating a fresh Leiningen plugin called " name "."))
    (->files data
             ["project.clj" (render "project.clj" data)]
             ["README.md" (render "README.md" data)]
             [".gitignore" (render "gitignore" data)]
             ["src/leiningen/{{sanitized}}.clj" (render "name.clj" data)])))
