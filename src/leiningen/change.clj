(ns leiningen.change
  "Rewrite project.clj by applying a function."
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [clojure.java.io :as io]
            [net.cgrand.sjacket :as sj]
            [net.cgrand.sjacket.parser :as parser]))

;;; Helpers

(defn- fail-argument! [msg]
  (throw (IllegalArgumentException. msg)))

(defn- clj->sjacket [value]
  (if (string? value)
    (str "\"" value "\"")
    (-> value print-str parser/parser :content first)))

;; NOTE: this destroy comments, formatting, etc.
(defn- sjacket->clj [value]
  (->> value sj/str-pt read-string))

(defn ^:internal normalize-path [value]
  (if (coll? value)
    value
    (map keyword (remove empty? (str/split value #":")))))

(defn ^:internal collapse-fn [f args]
  (let [f (cond (ifn? f) f
                (= "set" f) (constantly (first args))
                (string? f) (resolve (symbol f)))]
    #(apply f % args)))

;;; Traversal

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

(defn- find-key [loc key]
  (->> loc
       (iterate zip/right)
       (take-while (comp not nil?))
       (partition 2)
       (map first)
       (filter (comp #{key} sjacket->clj zip/node))
       first))

(defn- next-value [loc]
  (->> loc
       (iterate zip/right)
       (take-while (comp not nil?))
       (drop 1)
       (remove (comp #{:whitespace :comment} :tag zip/node))
       first))

(defn- parse-project [project-str]
  (-> (parser/parser project-str)
      zip/xml-zip
      find-defproject
      (or (fail-argument! "Project definition not found"))
      zip/up))

;;; Modifiers

(defn- insert-entry [loc val]
  (-> (if-not (= "{" (zip/node (zip/left loc)))
        (zip/insert-left loc " ")
        loc)
      (zip/insert-left (clj->sjacket val))))

(defn- update-version [proj fn]
  (-> proj
      find-string
      (or (fail-argument! "Project version not found"))
      (zip/edit fn)
      zip/root))

(defn- update-setting [loc [p & ath] fn]
  (let [loc'  (-> loc (find-key p) next-value)
        loc'' (or loc' (-> loc
                           zip/rightmost
                           (insert-entry p)
                           (insert-entry {})
                           zip/left))]
    (if (empty? ath)
      (zip/root (zip/edit loc'' fn ))
      (recur (-> loc'' zip/down zip/right) ath fn))))

;;; Public API

(defn change-string
  [project-str key-or-path f & args]
  (let [f (collapse-fn f args)
        wrapped-f (comp clj->sjacket f sjacket->clj)
        path (normalize-path key-or-path)
        proj (parse-project project-str)]
    (sj/str-pt
     ;; TODO: support :artifact-id, :group-id
     (if (= path [:version])
       (update-version proj wrapped-f)
       (update-setting proj path wrapped-f)))))

(defn change
  "Rewrite project.clj with f applied to the value at key-or-path.
  TODO: document accepted args."
  [project key-or-path f & args]
  ;; cannot work with project map, want to preserve formatting, comments, etc
  (let [source (slurp (io/file (:root project) "project.clj"))]
    (spit "project.clj" (apply change-string source key-or-path f args))))
