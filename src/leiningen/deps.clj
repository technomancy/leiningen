(ns leiningen.deps
  "Download all dependencies."
  (:require [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [leiningen.core.user :as user]
            [clojure.pprint :as pprint]
            [leiningen.core.utils :as utils]
            [cemerick.pomegranate.aether :as aether])
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

(defn- print-path [steps]
  (doseq [[dep version level] steps]
    (print-dep [dep version] level)))

(defn- why-finder [target]
  (let [path (atom [])]
    (fn [[dep version] level]
      (cond (= target dep) (print-path (conj @path [dep version level]))
            (< (count @path) (inc level)) (swap! path conj [dep version level])
            (= (count @path) (inc level)) (do (swap! path pop)
                                              (swap! path conj
                                                     [dep version level]))
            (> (count @path) (inc level)) (swap! path pop)))))



(declare check-signature)

(defn- fetch-key [signature err]
  (if (or (re-find #"Can't check signature: public key not found" err)
          (re-find #"Can't check signature: No public key" err))
    (let [key (second (re-find #"using \w+ key ID (.+)" err))
          {:keys [exit]} (user/gpg "--recv-keys" "--" key)]
      (if (zero? exit)
        (check-signature signature)
        :no-key))
    :bad-signature))

(defn- check-signature [signature]
  (let [{:keys [err exit]} (user/gpg "--verify" "--" (str signature))]
    (if (zero? exit)
      :signed ; TODO distinguish between signed and trusted
      (fetch-key signature err))))

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


(def tree-command
  "A mapping from the tree-command to the dependency key it should print a tree
  for."
  {":tree" [:dependencies :managed-dependencies]
   ":tree-data" [:dependencies :managed-dependencies]
   ":plugin-tree" [:plugins nil]})



(defn print-implicits [project type]
  (when-let [implicits (seq (filter utils/require-resolve
                                    (project/plugin-vars project type)))]
    (println (str "Implicit " (name type) ":"))
    (doseq [i implicits] (println " " i))))



(defn deps
  "Show details about dependencies.

    lein deps :tree

Show the full dependency tree for the current project. Each dependency is only
shown once within a tree.

    lein deps :tree-data

Show the full dependency tree as EDN for the current project. Each dependency 
is only shown once within a tree.

    lein deps :plugin-tree

Show the full dependency tree for the plugins in the current project.

    lein deps :verify

Check signatures of each dependency. ALPHA: subject to change.

    lein deps :implicits

List the implicit middleware and hooks that will be activated by the current
set of plugins. Useful for debugging unexplained behaviour.

    lein deps

Force Leiningen to download the dependencies it needs. This usage is
deprecated as it should happen automatically on demand.

Normally snapshot dependencies will be checked once every 24 hours; to
force them to be updated, use `lein -U $TASK`."
  ([project]
     (deps project nil))
  ([project command]
     (try
       (cond (= ":implicits" command)
             (do (print-implicits project :middleware)
                 (print-implicits project :hooks))
             (tree-command command)
             (let [project (project/merge-profiles
                            project
                            [{:pedantic? (quote ^:displace warn)}])
                   [dependencies-key managed-dependencies-key] (tree-command command)
                   hierarchy (classpath/managed-dependency-hierarchy
                              dependencies-key
                              managed-dependencies-key
                              project)]
               (case command
                 ":tree" (walk-deps hierarchy print-dep)
                 ":plugin-tree" (walk-deps hierarchy print-dep)
                 ":tree-data"  (binding [*print-length* 10000 *print-level* 10000]
                                 (pprint/pprint hierarchy))))
             (= command ":verify")
             (if (user/gpg-available?)
               (walk-deps (classpath/managed-dependency-hierarchy
                           :dependencies
                           :managed-dependencies
                           project)
                          (partial verify project))
               (main/abort (str "Could not verify - gpg not available.\n"
                                "See `lein help gpg` for how to setup gpg.")))
             :else (classpath/resolve-managed-dependencies
                    :dependencies :managed-dependencies project))
       (catch DependencyResolutionException e
         (main/abort (.getMessage e)))))
  ([project command target]
   (when-not (re-find #":why+" command)
     (main/abort "Unknown command" command))
   (walk-deps (classpath/managed-dependency-hierarchy :dependencies
                                                      :managed-dependencies
                                                      project)
              (why-finder (symbol target)))))
