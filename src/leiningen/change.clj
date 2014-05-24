(ns leiningen.change
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [net.cgrand.sjacket :refer [str-pt]]
            [net.cgrand.sjacket.parser :refer [parser]]))

;;-- Helpers

(defn- wrap-string [str] ["\"" str "\""])

(defn- unwrap-string [[_ str _]] str)

(defn- lookup-var [prefix task-name]
  (->> task-name
       name
       (str "leiningen.change/" prefix "-")
       symbol
       find-var))

(defn- fail-argument! [msg]
  (throw (IllegalArgumentException. msg)))

;;-- Traversal

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

(defn- get-project [project-str]
  (-> (parser project-str)
      zip/xml-zip
      find-defproject
      (or (fail-argument! "Project definition not found"))
      zip/up))

;;-- Modifiers

(defn- swap-version [project-str fn & args]
  (str-pt (-> (get-project project-str)
              find-string
              (or (fail-argument! "Project version not found"))
              (#(apply zip/edit % fn args))
              zip/root)))

;; TODO: the regular case, eg [:description]

;; TODO: the nested case, eg [:license :name]

(defn- run-reset-str [target value]
  (assoc target :content (wrap-string value)))

(defn- run-swap-str [target fn & args]
  (update-in target [:content] (comp wrap-string fn unwrap-string)))

;;; Public API

(defn change*
  [project-str key & [fn & args]]
  (if-let [swap-key (lookup-var "swap" key)]
    (let [fn' (if (fn? fn) fn (lookup-var "run" fn))]
      (apply swap-key project-str fn' args))
    (fail-argument! (str "Do not currently support changing :" (name key)))))

(defn change
  "Rewrite project.clj with new settings"
  [project key & args]
  ;; cannot work with project, as want to preserve formatting, comments, etc
  (let [source (slurp "project.clj")]
    (spit "project.clj" (apply change* source key args))))


;;; SANDBOX
;;; useful for driving dev, too naive an implementation

(defn- bump-version [version]
  (let [[major minor patch meta] (str/split version #"\.|\-")
        new-patch (inc (Long/parseLong patch))]
    (format "%s.%s.%d-%s" major minor new-patch meta)))

;; note the type awkwardness here.
;; we should probably just go in/out through sjacket's reader/parser, always

(defn run-bump-version [target]
  (run-swap-str target bump-version))
