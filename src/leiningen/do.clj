(ns leiningen.do
  (:refer-clojure :exclude [do])
  (:require [leiningen.core.main :as main]))

(defn- conj-to-last [coll x]
  (update-in coll [(dec (count coll))] conj x))

(defn ^:internal group-args
  ([args] (reduce group-args [[]] args))
  ([groups arg]
     (if (.endsWith arg ",")
       (-> groups
           (conj-to-last (subs arg 0 (dec (count arg))))
           (conj []))
       (conj-to-last groups arg))))

(defn ^:no-project-needed do
  "Perform comma-separated tasks in succession."
  [project & args]
  (doseq [[task-name & args] (group-args args)]
    (main/apply-task (main/lookup-alias task-name project) project args)))