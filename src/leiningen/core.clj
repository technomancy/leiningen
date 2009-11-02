(ns leiningen.core
  (:require [leiningen deps test compile jar uberjar help])
  (:gen-class))

(defn -main [command & args]
  (let [action (ns-resolve (symbol (str "leiningen." command))
                           (symbol command))]
    (apply action (leiningen/read-project) args)))