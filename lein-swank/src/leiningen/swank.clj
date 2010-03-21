(ns leiningen.swank
  (:use [leiningen.compile :only [eval-in-project]]))

(defn swank
  "Launch swank server for Emacs to connect. Optionally takes PORT and HOST."
  ([project port host & opts]
     (eval-in-project project
                      `(do (try (require '~'swank.swank)
                                (@(ns-resolve '~'swank.swank
                                              '~'start-repl)
                                 (Integer. ~port)
                                 ~@(concat (map read-string opts) [:host host]))
                                (catch Exception e#
                                  (println e#)
                                  (println "Make sure swank-clojure is added as"
                                           "a dev-dependency in your"
                                           "project.clj."))))))
  ([project port] (swank project port "localhost"))
  ([project] (swank project 4005)))
