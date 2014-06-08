(ns leiningen.echo)

(defn ^:no-project-needed echo [project & args]
  (apply println args))
