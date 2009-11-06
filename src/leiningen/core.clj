(ns leiningen.core
  (:require [leiningen deps test compile]
            [clojure.contrib.with-ns])
  (:gen-class))

(def project nil)

(defmacro defproject [project-name & args]
  ;; This is necessary since we must allow defproject to be eval'd in
  ;; any namespace due to load-file; we can't just create a var with
  ;; def or we would not have access to it once load-file returned.
  `(do (alter-var-root #'project
                       (fn [_#] (assoc (apply hash-map (quote ~args))
                                  :name ~(name project-name)
                                  :root ~(.getParent (java.io.File. *file*)))))
       (def ~project-name project)))

(defn read-project
  ([file] (load-file file)
     project)
  ([] (read-project "project.clj")))

(defn -main [command & args]
  (let [action (ns-resolve (symbol (str "leiningen." command))
                           (symbol command))]
    ;; TODO: ensure tasks run only once
    (apply action (read-project) args)
    ;; In case tests or some other task started any:
    (shutdown-agents)))