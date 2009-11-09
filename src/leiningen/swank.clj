(ns leiningen.swank
  (:use [clojure.contrib.logging :only [error]]))

(defn swank
  "Launch swank server for Emacs to connect."
  [project & [port]]
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
