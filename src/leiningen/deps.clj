(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [cemerick.pomegranate.aether :as aether]
            [clojure.pprint :as pp]
            [clojure.java.io :as io])
  (:import (org.sonatype.aether.resolution DependencyResolutionException)))

(defn- walk-deps
  ([deps f level]
     (doseq [[dep subdeps] deps]
       (f dep level)
       (when subdeps
         (walk-deps subdeps f (inc level)))))
  ([deps f]
     (walk-deps deps f 0)))

(defn- print-dep [dep level]
  (println (str (apply str (repeat (* 2 level) \space))) (pr-str dep)))

(declare check-signature)

(defn- fetch-key [signature err]
  (if (re-find #"Can't check signature: public key not found" err)
    (let [key (second (re-find #"using \w+ key ID (.+)" err))
          exit (eval/sh "gpg" "--recv-keys" key)]
      (if (zero? exit)
        (check-signature signature)
        :no-key))
    :bad-signature))

(defn- check-signature [signature]
  (let [err (java.io.StringWriter.)
        out (java.io.StringWriter.)
        exit (binding [*err* (java.io.PrintWriter. err), *out* out]
               (eval/sh "gpg" "--verify" (str signature)))]
    (if (zero? exit)
      :signed
      (fetch-key signature (str err)))))

(defn- get-signature [project dep]
  (let [dep-map (assoc (apply hash-map (drop 2 dep))
                  :extension "jar.asc")
        dep (into (vec (take 2 dep)) (apply concat dep-map))]
    (try (->> (aether/resolve-dependencies
               :repositories (:repositories project)
               :coordinates [dep])
              (aether/dependency-files)
              (filter #(.endsWith (.getName %) ".asc"))
              (first))
         (catch DependencyResolutionException _))))

(defn- verify [project dep _]
  (let [signature (get-signature project dep)
        status (if signature
                 (check-signature signature)
                 :unsigned)]
    (println status (pr-str dep))))

(defn deps
  "Download or show all dependencies.

To show the full dependency tree for the current project, run:

    lein deps :tree

To manually have Leiningen download all missing or out-of-date
dependencies, you could run `lein deps`, but that's generally not
necessary, since Leiningen automatically checks for and downloads
those."
  ([project]
     (deps project nil))
  ([project style]
     (try
       (cond (= style ":tree")
             (walk-deps (classpath/dependency-hierarchy :dependencies project)
                        print-dep)
             (= style ":verify")
             (walk-deps (classpath/dependency-hierarchy :dependencies project)
                        (partial verify project))
             :else (classpath/resolve-dependencies :dependencies project))
       (catch DependencyResolutionException e
         (main/abort (.getMessage e))))))
