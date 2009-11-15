(ns leiningen.swank
  (:use [leiningen.deps :only [deps-if-missing]]
        [clojure.contrib.logging :only [error]]))

;; TODO: This should be spun off into an external task jar
(defn swank
  "Launch swank server for Emacs to connect."
  [project & [port]]
  (deps-if-missing project)
  (try (require 'swank.swank)
       ;; Can't compile code that refers to a var that may not exist:
       (let [repl @(ns-resolve 'swank.swank 'start-repl)]
         (if port
           (repl (Integer. port))
           (repl)))
       (catch java.io.FileNotFoundException _
         (error (str "Couldn't load swank-clojure. "
                     "Did you add it as a project dependency?"))
         (System/exit 1))))
