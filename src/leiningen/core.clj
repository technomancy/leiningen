(ns leiningen.core
  (:use [clojure.contrib.with-ns])
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

;; So it doesn't need to be fully-qualified in project.clj
(with-ns 'user (use ['leiningen.core :only ['defproject]]))

(defn read-project
  ([file] (load-file file)
     project)
  ([] (read-project "project.clj")))

(defn -main [command & args]
  (let [action-ns (symbol (str "leiningen." command))
        _ (require action-ns)
        action (ns-resolve action-ns (symbol command))
        project (read-project)]
    (binding [*compile-path* (or (:compile-path project)
                                 (str (:root project) "/classes/"))]
      ;; TODO: ensure tasks run only once
      (apply action project args)
      ;; In case tests or some other task started any:
      (shutdown-agents))))