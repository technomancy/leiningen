(ns leiningen.project)

(defn ^:no-project-needed project [project & args]
  (if (seq args)
    (get-in project (mapv keyword args))
    project))
