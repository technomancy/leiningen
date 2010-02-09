(ns leiningen.swank
  (:use [leiningen.compile :only [eval-in-project]]))

(defn swank
  "Launch swank server for Emacs to connect."
  ([project port & opts]
     (eval-in-project project
                      `(do (try (require '~'swank.swank)
                                (@(ns-resolve '~'swank.swank
                                              '~'start-repl)
                                 (Integer. ~port)
				 ~@(map read-string opts))
                                (catch Exception e#
                                  (println e#)
                                  (println "Make sure swank-clojure is added as"
                                           "a dev-dependency in your"
                                           "project.clj."))))))
  ([project] (swank project 4005)))
