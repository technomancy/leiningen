(ns leiningen.core
  (:require [leiningen deps test compile jar uberjar help]
            [clojure.contrib.with-ns])
  (:gen-class))

(declare project)

(defmacro defproject [project-name & args]
  `(do (def ~project-name (assoc (apply hash-map (quote ~args))
                            :name ~(name project-name)
                            :root ~(.getParent (java.io.File. *file*))))
       (let [project# ~project-name]
         (with-ns 'leiningen.core
           (def '~'project project#)))))

(defn read-project []
  (load-file "build.clj")
  project)

(defn -main [command & args]
  (let [action (ns-resolve (symbol (str "leiningen." command))
                           (symbol command))]
    (apply action (read-project) args)))