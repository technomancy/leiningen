(ns leiningen.change
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [net.cgrand.sjacket :refer [str-pt]]
            [net.cgrand.sjacket.parser :refer [parser]]))

;;-- helpers

(defn- wrap-string [str] ["\"" str "\""])

(defn- unwrap-string [[_ str _]] str)

(defn- bump-version [version]
  ;; NOTE: technically http://semver.org/ defines 'prelease' and 'meta' data
  ;; TODO: better error handling here (wrong structure? not a number?)
  (let [[major minor patch meta] (str/split version #"\.|\-")
        new-patch (inc (Long/parseLong patch))]
    (format "%s.%s.%d-%s" major minor new-patch meta)))

(defn- fail-argument! [msg]
  (throw (IllegalArgumentException. msg)))

;;-- traversal

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

;;-- mutation

(defn- swap-version [project-str fn & args]
  (str-pt (-> (get-project project-str)
              find-string
              (or (fail-argument! "Project version not found"))
              (#(apply zip/edit % fn args))
              zip/root)))

;;-- tasks

(defn- run-version
  "Replace the project version"
  [project-str & [version]]
  (swap-version project-str assoc :content (wrap-string version)))

(defn- run-bump-version
  "Update the project version's patch number"
  [project-str]
  (swap-version project-str update-in [:content] (comp wrap-string
                                                       bump-version
                                                       unwrap-string)))

(defn- lookup-task [task-name]
  (->> task-name
       name
       (str "leiningen.change/run-")
       symbol
       find-var))

(defn change*
  [project-str task & args]
  (if-let [task-fn (lookup-task task)]
    (apply task-fn project-str args)
    (fail-argument! "Only support changing :version for now")))

(defn change
  "Rewrite project.clj with new settings"
  [project task & args]
  ;; cannot work with project, as want to preserve formatting, comments, etc
  (let [source (slurp "project.clj")]
    (spit "project.clj" (apply change* source task args))))
