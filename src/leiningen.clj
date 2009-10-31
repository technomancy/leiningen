(ns leiningen
  (:use [lancet])
  )

(require)

(defmacro defproject [project-name & args]
  `(def ~project-name (assoc (apply hash-map (quote ~args))
                :name ~(name project-name))))

(deftarget deps "Download all the project's dependencies into lib/")

