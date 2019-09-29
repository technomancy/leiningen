(ns leiningen.do
  "Higher-order task to perform other tasks in succession."
  (:refer-clojure :exclude [do])
  (:require [leiningen.core.main :as main]))

(defn- conj-to-last [coll x]
  (update-in coll [(dec (count coll))] conj x))

(defn- butlast-char
  "Removes the last character in the string."
  [s]
  (subs s 0 (dec (count s))))

(defn- pop-if-last
  "Pops the collection if (pred (peek coll)) is truthy."
  [coll pred]
  (if (pred (peek coll))
    (pop coll)
    coll))

(defn ^:internal group-args
  ([args]  (-> (reduce group-args [[]] args)
               (pop-if-last empty?)))
  ([groups arg]
   (cond (coll? arg) (-> (pop-if-last groups empty?)
                         (conj arg []))
         (.endsWith arg ",") (-> groups
                                 (conj-to-last (butlast-char arg))
                                 (conj []))
         :else (conj-to-last groups arg))))

(defn ^:no-project-needed ^:higher-order do
  "Higher-order task to perform other tasks in succession.

Each comma-separated group should be a task name followed by optional arguments.

USAGE: lein do test, compile :all, deploy private-repo"
  [project & args]
  (doseq [arg-group (group-args args)]
    (main/resolve-and-apply project arg-group)))
