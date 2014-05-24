(ns leiningen.change
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [net.cgrand.sjacket :refer [str-pt]]
            [net.cgrand.sjacket.parser :refer [parser]]))

;;; Helpers

(defn- fail-argument! [msg]
  (throw (IllegalArgumentException. msg)))

(defn- clj->sjacket [value]
  (if (string? value)
    (str "\"" value "\"")
    (-> value print-str parser :content first)))

;; NOTE: this destroy comments, formatting, etc.
;; NOTE: read-string may throw parse errors on badly formed config..
;;       is this an issue or will files already have been sanity
;;       checked before this task can run?
(defn- sjacket->clj [value]
  (->> value str-pt read-string))

(defn- lookup-var [x]
  ;; ensure it's a namespaced var reference to avoid error
  (if (re-find #"^[a-zA-Z]+\..+\/.+$" x)
    (-> x symbol find-var var-get)))

(defn ^:internal normalize-path
  "Coerce scalars, colls and cli-encoded lists of symbols/strings into keyword vector"
  [value]
  (mapv keyword
        (if (coll? value)
          (map name value)
          (let [value (name value)]
            (if (re-find #":" (name value))
              (remove empty? (str/split (name value) #":"))
              [(name value)])))))

(defn ^:internal collapse-fn
  "Partially apply args to right if fn, else return constant of first arg.
   If string corresponds to a namespaced var, substite value for string"
  [fn args]
  (let [fn' (or (and (string? fn) (lookup-var fn)) fn)]
    (if (fn? fn')
      #(apply fn' % args)
      (constantly fn'))))

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

(defn- get-project [project-str]
  (-> (parser project-str)
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

(defn insert-key-val [loc key val]
  (-> loc
      (insert-entry key)
      (insert-entry val)))

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
                           (insert-key-val p {})
                           zip/left))]
    (if (empty? ath)
      (zip/root (zip/edit loc'' fn ))
      (recur (-> loc'' zip/down zip/right) ath fn))))

;;; Public API

(defn change*
  [project-str key-or-path fn & args]
  (let [fn'  (collapse-fn fn args)
        fn'' (comp clj->sjacket fn' sjacket->clj)
        path (normalize-path key-or-path)
        proj (get-project project-str)]
    (str-pt
     (if (= path [:version])
       (update-version proj fn'')
       (update-setting proj path fn'')))))

(defn change
  "Rewrite project.clj with new settings"
  [project & args]
  ;; cannot work with project, want to preserve formatting, comments, etc
  (let [source (slurp "project.clj")]
    (spit "project.clj" (apply change* source args))))
