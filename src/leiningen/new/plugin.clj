(ns leiningen.new.plugin
  (:require [leiningen.new.templates :refer [renderer sanitize year ->files]]
            [leiningen.core.main :as main]))

(defn plugin
  "A leiningen plugin project template."
  [^String name]
  (let [render (renderer "plugin")
        unprefixed (if (.startsWith name "lein-")
                     (subs name 5)
                     name)
        data {:name name
              :unprefixed-name unprefixed
              :sanitized (sanitize unprefixed)
              :year (year)}]
    (main/info (str "Generating a fresh Leiningen plugin called " name "."))
    (->files data
             ["project.clj" (render "project.clj" data)]
             ["README.md" (render "README.md" data)]
             [".gitignore" (render "gitignore" data)]
             [".hgignore" (render "hgignore" data)]
             ["src/leiningen/{{sanitized}}.clj" (render "name.clj" data)]
             ["LICENSE" (render "LICENSE" data)])))
