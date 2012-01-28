(ns leiningen.core.project
  "Read project.clj files."
  (:refer-clojure :exclude [read])
  (:require [clojure.walk :as walk]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [cemerick.pomegranate :as pomegranate]
            [leiningen.core.user :as user])
  (:import (clojure.lang DynamicClassLoader)))

(defn- unquote-project
  "Inside defproject forms, unquoting (~) allows for arbitrary evaluation."
  [args]
  (walk/walk (fn [item]
               (cond (and (seq? item) (= `unquote (first item))) (second item)
                     ;; needed if we want fn literals preserved
                     (or (seq? item) (symbol? item)) (list 'quote item)
                     :else (unquote-project item)))
             identity
             args))

(def defaults {:source-path ["src"]
               :resources-path ["resources"]
               :test-path []
               :native-path "native"
               :compile-path "target/classes"
               :target-path "target"
               :repositories [["central" "http://repo1.maven.org/maven2"]
                              ;; TODO: point to releases-only before 2.0 is out
                              ["clojars" "http://clojars.org/repo/"]]
               :jar-exclusions [#"^\."]
               :uberjar-exclusions [#"^META-INF/DUMMY.SF"]})

(defn ^:internal add-repositories
  "Public only for macroexpansion purposes, :repositories needs special
  casing logic for merging default values with user-provided ones."
  [{:keys [omit-default-repositories repositories] :as
    project}]
  (assoc project :repositories
         (for [[id repo] (concat repositories (if-not omit-default-repositories
                                                (:repositories defaults)))]
           [id (if (string? repo) {:url repo} repo)])))

(defmacro defproject
  "The project.clj file must either def a project map or call this macro."
  [project-name version & {:as args}]
  `(let [args# ~(unquote-project args)]
     (def ~'project
       (merge defaults (dissoc (add-repositories args#)
                               ;; Strip out aliases for normalization.
                               :eval-in-leiningen :deps)
              {:name ~(name project-name)
               :group ~(or (namespace project-name)
                           (name project-name))
               :version ~version
               :dependencies (or (:dependencies args#) (:deps args#))
               :compile-path (or (:compile-path args#)
                                 (.getPath (io/file (:target-path args#)
                                                    "classes")))
               :root ~(.getParent (io/file *file*))
               :eval-in (or (:eval-in args#)
                            (if (:eval-in-leiningen args#)
                              :leiningen
                              :subprocess))}))))

(def default-profiles
  "Profiles get merged into the project map. The :dev and :user
  profiles are active by default."
  (atom {:dev {:test-path ["test"]
               :resources-path ["dev-resources"]}
         :debug {:debug true}}))

;; Modified merge-with to provide f with the conflicting key.
(defn- merge-with-key [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
                        (let [k (key e) v (val e)]
                          (if (contains? m k)
                            (assoc m k (f k (get m k) v))
                            (assoc m k v))))
          merge2 (fn [m1 m2]
                   (reduce merge-entry (or m1 {}) (seq m2)))]
      (reduce merge2 maps))))

;; TODO: This would just be a merge if we had an ordered map
(defn- merge-dependencies [result latter]
  (let [latter-deps (set (map first latter))]
    (concat latter (remove (comp latter-deps first) result))))

(defn- profile-key-merge
  "Merge profile values into the project map based on their type."
  [key result latter]
  (cond (-> result meta :displace)
        latter

        (-> latter meta :replace)
        latter

        (= :dependencies key)
        (merge-dependencies result latter)

        (= :repositories key)
        (concat (seq result) (seq latter))

        (and (map? result) (map? latter))
        (merge-with-key profile-key-merge latter result)

        (and (set? result) (set? latter))
        (set/union latter result)

        (and (coll? result) (coll? latter))
        (concat latter result)

        :else (doto latter (prn :profile-merge-else))))

(defn- merge-profile [project profile]
  (merge-with-key profile-key-merge project profile))

(defn- lookup-profile [profiles profile]
  (let [result (profiles profile)]
    (if (keyword? result)
      (recur profiles result)
      result)))

(defn- profiles-for
  "Read profiles from a variety of sources.

  We check Leiningen's defaults, the profiles.clj file in ~/.lein/profiles.clj,
  the profiles.clj file in the project root, and the :profiles key from the
  project map."
  [project profiles-to-apply]
  (let [profiles (merge @default-profiles (user/profiles) (:profiles project))]
    ;; We reverse because we want profile values to override the
    ;; project, so we need "last wins" in the reduce, but we want the
    ;; first profile specified by the user to take precedence.
    (map (partial lookup-profile profiles) (reverse profiles-to-apply))))

(defn merge-profiles
  "Look up and merge the given profile names into the project map."
  [project profiles-to-apply]
  (with-meta (reduce merge-profile project
                     (profiles-for project profiles-to-apply))
    {:without-profiles project}))

(defn- absolutize [root path]
  (if (coll? path) ; paths can be either strings or collections of strings
    (map (partial absolutize root) path)
    (str (io/file root path))))

(defn- absolutize-path [project key]
  (if (re-find #"-path$" (name key))
    (update-in project [key] (partial absolutize (:root project)))
    project))

(defn- absolutize-paths [project]
  (reduce absolutize-path project (keys project)))


(defn ensure-dynamic-classloader []
  (let [thread (Thread/currentThread)
        cl (.getContextClassLoader thread)]
    (when-not (instance? DynamicClassLoader cl)
      (.setContextClassLoader thread (DynamicClassLoader. cl)))))

(defn load-plugins [project]
  (ensure-dynamic-classloader)
  (when (= :leiningen (:eval-in project))
    (pomegranate/add-dependencies (:dependencies project)
                                  :repositories (:repositories project)))
  (pomegranate/add-dependencies
   (:plugins project) :repositories (:repositories project)))

(defn read
  "Read project map out of file, which defaults to project.clj."
  ([file profiles]
     (binding [*ns* (find-ns 'leiningen.core.project)]
       (load-file file))
     (let [project (resolve 'leiningen.core.project/project)]
       (when-not project
         (throw (Exception. "project.clj must define project map.")))
       (ns-unmap *ns* 'project) ; return it to original state
       (let [project (merge-profiles @project profiles)]
         (load-plugins project)
         (absolutize-paths project))))
  ([file] (read file [:dev :user]))
  ([] (read "project.clj")))
