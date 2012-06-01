(ns leiningen.core.project
  "Read project.clj files."
  (:refer-clojure :exclude [read])
  (:require [clojure.walk :as walk]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [ordered.map :as ordered]
            [cemerick.pomegranate :as pomegranate]
            [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath])
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

(def defaults {:source-paths ["src"]
               :resource-paths ["resources"]
               :test-paths ["test"]
               :native-path "target/native"
               :compile-path "target/classes"
               :target-path "target"
               :prep-tasks ["javac" "compile"]
               :repositories (ordered/ordered-map
                              "central" {:url "http://repo1.maven.org/maven2"}
                              ;; TODO: point to releases-only before 2.0 is out
                              "clojars" {:url (if (= "Windows"
                                                     (System/getProperty "os.name"))
                                                "http://clojars.org/repo/"
                                                "https://clojars.org/repo/")})
               :jar-exclusions [#"^\."]
               :uberjar-exclusions [#"(?i)^META-INF/[^/]*\.(SF|RSA)$"]})

(defmacro defproject
  "The project.clj file must either def a project map or call this macro."
  [project-name version & {:as args}]
  `(let [args# ~(unquote-project args)]
     (def ~'project
       (merge defaults args#
              {:name ~(name project-name)
               :group ~(or (namespace project-name)
                           (name project-name))
               :version ~version
               :root ~(.getParent (io/file *file*))
               :eval-in (or (:eval-in args#)
                            (if (:eval-in-leiningen args#)
                              :leiningen
                              :subprocess))}))))

(defn normalize-repos [{:keys [omit-default-repositories
                               repositories] :as project}]
  (assoc project :repositories
         (into (if-not omit-default-repositories
                 (:repositories defaults)
                 (ordered/ordered-map))
               (for [[id repo] repositories]
                 [id (if (map? repo) repo {:url repo})]))))

(defn- dedupe-step [[deps seen] x]
  (if (seen (first x))
    ;; this would be so much cleaner if we could just re-use profile-merge
    ;; logic, but since :dependencies are a vector, the :replace/:displace
    ;; calculations don't apply to nested vectors inside :dependencies.
    (let [[seen-dep] (filter #(= (first %) (first x)) deps)]
      (if (or (:displace (meta seen-dep)) (:replace (meta x)))
        [(assoc deps (.indexOf deps seen-dep) x) seen]
        [deps seen]))
    [(conj deps x) (conj seen (first x))]))

(defn- dedupe-deps [deps]
  (first (reduce dedupe-step [[] #{}] deps)))

(defn- exclude [exclusions deps dep]
  (conj deps
        (if (empty? exclusions)
          dep
          (let [exclusions-offset (.indexOf dep :exclusions)]
            (if (pos? exclusions-offset)
              (update-in dep [(inc exclusions-offset)]
                         (comp vec distinct (partial into exclusions)))
              (-> dep
                  (conj :exclusions)
                  (conj exclusions)))))))

(defn- add-exclusions [deps exclusions]
  (reduce (partial exclude exclusions) [] deps))

(defn normalize-deps [project]
  (-> project
      (update-in [:dependencies] dedupe-deps)
      (update-in [:dependencies] add-exclusions (:exclusions project))))

(defn normalize-plugins [project]
  (update-in project [:plugins] dedupe-deps))

(defn- absolutize [root path]
  (str (if (.isAbsolute (io/file path))
         path
         (io/file root path))))

(defn- absolutize-path [{:keys [root] :as project} key]
  (cond (re-find #"-path$" (name key))
        (update-in project [key] (partial absolutize root))

        (re-find #"-paths$" (name key))
        (update-in project [key] (partial map (partial absolutize root)))

        :else project))

(defn absolutize-paths [project]
  (let [project (reduce absolutize-path project (keys project))]
    (assoc project :compile-path (or (:compile-path project)
                                     (str (io/file (:target-path project)
                                                   "classes"))))))

(defn remove-aliases [project]
  (dissoc project :deps :eval-in-leiningen))

(def ^{:arglists '([project])} normalize
  "Normalize project map to standard representation."
  (comp normalize-repos normalize-deps absolutize-paths remove-aliases))

(def default-profiles
  "Profiles get merged into the project map. The :dev and :user
  profiles are active by default."
  (atom {:default {:resource-paths ["dev-resources"]
                   :plugins [['lein-newnew "0.3.1"]
                             ['reply "0.1.0-beta8"]]
                   :jvm-opts ["-XX:+TieredCompilation"]
                   :checkout-deps-shares [:source-paths
                                          :resource-paths
                                          :compile-path]}
         :production {}
         :test {}
         :update {:update :always}
         :offline {:offline? true}
         :debug {:debug true}}))

(defn- profile-key-merge
  "Merge profile values into the project map based on their type."
  [result latter]
  (cond (-> result meta :displace)
        latter

        (-> latter meta :replace)
        latter

        (and (map? result) (map? latter))
        (merge-with profile-key-merge latter result)

        (and (set? result) (set? latter))
        (set/union latter result)

        (and (coll? result) (coll? latter))
        (concat latter result)

        (= (class result) (class latter)) latter

        :else (doto latter (println "has a type mismatch merging profiles."))))

(defn- merge-profile [project profile]
  (vary-meta (merge-with profile-key-merge project profile)
             update-in [:included-profiles] conj profile))

(defn- lookup-profile [profiles profile-name]
  (let [result (profiles profile-name)]
    ;; TODO: only warn when profiles are explicitly requested
    (when (and (nil? result) (not (#{:default :dev :user :test} profile-name)))
      (println "Warning: profile" profile-name "not found."))
    (if (keyword? result)
      (recur profiles result)
      result)))

(defn- profiles-for
  "Read profiles from a variety of sources.

  We check Leiningen's defaults, the profiles.clj file in ~/.lein/profiles.clj,
  the profiles.clj file in the project root, and the :profiles key from the
  project map."
  [project profiles-to-apply]
  (when (some (comp :repositories val) (user/profiles))
    (println "WARNING: :repositories detected in user-level profile!")
    (println "See https://github.com/technomancy/leiningen/wiki/Repeatability"))
  (let [profiles (merge @default-profiles (user/profiles) (:profiles project))]
    ;; We reverse because we want profile values to override the
    ;; project, so we need "last wins" in the reduce, but we want the
    ;; first profile specified by the user to take precedence.
    (map #(if (keyword? %) (lookup-profile profiles %) %)
         (reverse profiles-to-apply))))

(defn ensure-dynamic-classloader []
  (let [thread (Thread/currentThread)
        cl (.getContextClassLoader thread)]
    (when-not (pomegranate/modifiable-classloader? cl)
      (.setContextClassLoader thread (DynamicClassLoader. cl)))))

(defn load-plugins
  ([project key]
     (when (seq (project key))
       (ensure-dynamic-classloader)
       (classpath/resolve-dependencies key project :add-classpath? true)))
  ([project] (load-plugins project :plugins)))

(defn- load-hooks [project]
  (doseq [n (:hooks project)]
    (try (require n)
         (when-let [activate (ns-resolve n 'activate)]
           (activate))
         (catch Throwable e
           (binding [*out* *err*]
             (println "Error: problem requiring" n "hook"))
           (throw e)))))

(defn apply-middleware [project middleware-name]
  (when-let [m-ns (namespace middleware-name)]
    (require (symbol m-ns)))
  ((resolve middleware-name) project))

(defn init-project
  "Initializes a project: loads plugins and hooks.
   Adds dependencies to Leiningen's classpath if required."
  [project]
  (when (= :leiningen (:eval-in project))
    (doseq [path (classpath/get-classpath project)]
      (pomegranate/add-classpath path)))
  (load-plugins project)
  (load-hooks project)
  project)

(defn merge-profiles
  "Look up and merge the given profile names into the project map."
  [project profiles-to-apply]
  (let [merged (reduce merge-profile project
                       (profiles-for project profiles-to-apply))]
    (vary-meta (normalize merged) merge
               {:without-profiles (normalize (:without-profiles (meta project) project))
                :included-profiles (concat (:included-profiles (meta project))
                                           profiles-to-apply)})))

(defn ^{:deprecated "2.0.0-preview3"} conj-dependency
  "Add a dependency into the project map if it's not already present. Warn the
  user if it is. Plugins should use this rather than altering :dependencies.

  Deprecated in 2.0.0-preview3."
  [project dependency]
  (println "WARNING: conj-dependencies is deprecated.")
  (update-in project [:dependencies] conj dependency))

(defn add-profiles
  "Add the profiles in the given profiles map to the project map, taking care
   to preserve project map metadata. Note that these profiles are not merged,
   merely made available to merge by name."
  [project profiles-map]
  ;; Merge new profiles into both the project and without-profiles meta
  (vary-meta (update-in project [:profiles] merge profiles-map)
             merge
             {:without-profiles (update-in (:without-profiles (meta project)
                                                              project)
                                           [:profiles] merge
                                           profiles-map)}))

(defn unmerge-profiles
  "Given a project map, return the project map you would have if the specified
   profiles had never been merged into it. Expects a list of profiles, where
   each element is either the name of a profile in the :profiles key of the
   project, or the map of the profile itself."
  [project profiles-to-unmerge]
  (let [result-profiles (filter (comp not (into #{} profiles-to-unmerge))
                                (:included-profiles (meta project)))]
    (merge-profiles (:without-profiles (meta project) project)
                    result-profiles)))

(defn read
  "Read project map out of file, which defaults to project.clj."
  ([file profiles]
     (locking read
       (binding [*ns* (find-ns 'leiningen.core.project)]
         (load-file file))
       (let [project (or (resolve 'leiningen.core.project/project)
                         (throw
                          (Exception. "project.clj must define project map.")))]
         ;; return it to original state
         (ns-unmap 'leiningen.core.project 'project)
         (-> (reduce apply-middleware @project (:middleware @project))
             (merge-profiles profiles)))))
  ([file] (read file [:dev :user :default]))
  ([] (read "project.clj")))
