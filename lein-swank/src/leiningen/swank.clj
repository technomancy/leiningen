(ns leiningen.swank
  (:use [leiningen.deps :only [deps-if-missing]]))

(defn swank
  "Launch swank server for Emacs to connect."
  [project & [port]]
  (deps-if-missing project)
  (let [repl @(ns-resolve 'swank.swank 'start-repl)]
         (if port
           (repl (Integer. port))
           (repl))))
