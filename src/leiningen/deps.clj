(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [leiningen.core.user :as user]
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
  (println (apply str (repeat (* 2 level) \space)) (pr-str dep)))

(declare check-signature)

(defn- fetch-key [signature err]
  (if (re-find #"Can't check signature: public key not found" err)
    (let [key (second (re-find #"using \w+ key ID (.+)" err))
          exit (eval/sh (user/gpg-program) "--recv-keys" key)]
      (if (zero? exit)
        (check-signature signature)
        :no-key))
    :bad-signature))

(defn- check-signature [signature]
  (let [err (java.io.StringWriter.)
        out (java.io.StringWriter.)
        exit (binding [*err* (java.io.PrintWriter. err), *out* out]
               (eval/sh (user/gpg-program) "--verify" (str signature)))]
    (if (zero? exit)
      :signed ; TODO distinguish between signed and trusted
      (fetch-key signature (str err)))))

(defn- get-signature [project dep]
  (let [dep-map (assoc (apply hash-map (drop 2 dep))
                  ;; TODO: check pom signature too
                  :extension "jar.asc")
        dep (into (vec (take 2 dep)) (apply concat dep-map))]
    (try (->> (aether/resolve-dependencies
               :repositories (:repositories project)
               :mirrors (:mirrors project)
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
    ;; TODO: support successful exit code only on fully-signed deps
    (println status (pr-str dep))))

(defn deps
  "Show details about dependencies.

USAGE: lein deps :tree
Show the full dependency tree for the current project.

USAGE: lein deps :verify
Check signatures of each dependency. ALPHA: subject to change.

USAGE: lein deps
Force Leiningen to download the dependencies it needs. This usage is
deprecated as it should happen automatically on demand.

Normally snapshot dependencies will be checked once every 24 hours; to
force them to be updated, use `lein -U $TASK`."
  ([project]
     (deps project nil))
  ([project command]
     (try
       (cond (= command ":tree")
             (walk-deps (classpath/dependency-hierarchy :dependencies project)
                        print-dep)
             (= command ":verify")
             (walk-deps (classpath/dependency-hierarchy :dependencies project)
                        (partial verify project))
             :else (classpath/resolve-dependencies :dependencies project))
       (catch DependencyResolutionException e
         (main/abort (.getMessage e))))))
