(ns leiningen.change
  (:require [clojure.zip :as zip]
            [net.cgrand.sjacket :refer [str-pt]]
            [net.cgrand.sjacket.parser :refer [parser]]))

(defn- fail-argument! [msg]
  (throw (IllegalArgumentException. msg)))

(defn- defproject? [loc]
  (let [{:keys [tag content]} (zip/node loc)]
    (and (= :name tag)
         (= ["defproject"] content))))

(defn- find-defproject [loc]
  (->> loc
       (iterate zip/next)
       (take-while (comp not zip/end?))
       (filter defproject?)
       first))

(defn- find-string [loc]
  (->> loc
      (iterate zip/right)
      (take-while (comp not nil?))
      (filter (comp #{:string} :tag zip/node))
      first))

(defn- change-version
  "Given project.clj string, replace the version"
  [project-str new-version]
  (let [tree    (parser project-str)
        zipper  (zip/xml-zip tree)
        content ["\"" new-version "\""]]
    (str-pt
     (-> zipper
         find-defproject
         (or (fail-argument! "Project definition not found"))
         zip/up
         find-string
         (or (fail-argument! "Project version not found"))
         (zip/edit assoc :content content)
         zip/root))))

(defn change*
  [project-str key value]
  (condp = (name key)
    "version" (change-version project-str value)
    (fail-argument! "Only support changing :version for now")))

(defn change
  "Rewrite project.clj with new settings"
  [project key value]
  ;; cannot work with project, as want to preserve formatting, comments, etc
  (let [source (slurp "project.clj")]
    (spit "project.clj" (change* source key value))))
