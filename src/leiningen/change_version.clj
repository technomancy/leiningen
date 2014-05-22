(ns leiningen.change-version
  (require [clojure.java.io :as io]
           [clojure.pprint :as pprint]))

(defn split-version [project-str]
  (if-let [match (re-find #"(?s)(.*\(defproject.*?\")([\d.]+(?:-\w+)?)(\".+)" project-str)]
    (rest match)
    (throw (Throwable. "Can't find version"))))

(defn change-version* [project-str new-version]
  (let [[preamble version rest] (split-version project-str)]
    (str preamble new-version rest)))

(defn change-version
  ([version]
   (change-version nil version))
  ([project version]
   (let [project-str (slurp "project.clj")]
     (spit "project.clj" (change-version* project-str version)))))
