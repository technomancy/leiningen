(ns leiningen.new.app
  "Generate a basic application project."
  (:use [leiningen.new.templates :only [renderer year project-name
                                        ->files sanitize-ns name-to-path
                                        multi-segment]]))

(defn app
  "An application project template."
  [name]
  (let [render (renderer "app")
        main-ns (multi-segment (sanitize-ns name))
        data {:raw-name name
              :name (project-name name)
              :namespace main-ns
              :nested-dirs (name-to-path main-ns)
              :year (year)}]
    (println "Generating a project called" name "based on the 'app' template.")
    (->files data
             ["project.clj" (render "project.clj" data)]
             ["README.md" (render "README.md" data)]
             ["doc/intro.md" (render "intro.md" data)]
             [".gitignore" (render "gitignore" data)]
             ["src/{{nested-dirs}}.clj" (render "core.clj" data)]
             ["test/{{nested-dirs}}_test.clj" (render "test.clj" data)])))
