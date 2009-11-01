(ns leiningen)

(defmacro defproject [project-name & args]
  `(def ~project-name (assoc (apply hash-map (quote ~args))
                        :name ~(name project-name)
                        :root ~(.getParent (java.io.File. *file*)))))

(defn read-project []
  @(load-file "build.clj"))