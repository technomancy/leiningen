(ns leiningen.core
  (:require [leiningen deps test compile]
            [clojure.contrib.with-ns])
  (:gen-class))

(def project nil)

(defmacro defproject [project-name & args]
  `(do (alter-var-root #'project
                       (fn [_#] (assoc (apply hash-map (quote ~args))
                                  :name ~(name project-name)
                                  :root ~(.getParent (java.io.File. *file*)))))
       (def ~project-name project)))

(defn read-project []
  (load-file "build.clj")
  project)

(defn -main [command & args]
  (let [action (ns-resolve (symbol (str "leiningen." command))
                           (symbol command))]
    ;; TODO: ensure tasks run only once
    (apply action (read-project) args)))