(ns leiningen.repl
  (:require [clojure.main])
  (:use [leiningen.compile :only [eval-in-project]]))

(defn repl [project]
  (let [repl-init (and (:main project)
                       [:init `#(doto '~(:main project) require in-ns)])]
    (eval-in-project project
                     `(clojure.main/repl ~@repl-init))))
