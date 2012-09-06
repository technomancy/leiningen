(ns leiningen.core.project
  "Read project.clj files."
  (:refer-clojure :exclude [read])
  (:require [clojure.walk :as walk]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [cemerick.pomegranate :as pomegranate]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.utils :as utils]
            [leiningen.core.ssl :as ssl]
            [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath])
  (:import (clojure.lang DynamicClassLoader)
           (java.io PushbackReader)))

;; # Project definition and normalization

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

(def ^:private hooke-injection
  (with-open [rdr (-> "robert/hooke.clj" io/resource io/reader PushbackReader.)]
    `(do (ns ~'leiningen.core.injected)
         ~@(doall (take 6 (rest (repeatedly #(clojure.core/read rdr)))))
         (ns ~'user))))

(def defaults {:source-paths ["src"]
               :resource-paths ["resources"]
               :test-paths ["test"]
               :native-path "target/native"
               :compile-path "target/classes"
               :target-path "target"
               :prep-tasks ["javac" "compile"]
               :repositories [["central" {:url "http://repo1.maven.org/maven2/"}]
                              ;; TODO: point to releases-only before 2.0 is out
                              ["clojars" {:url "https://clojars.org/repo/"}]]
               :deploy-repositories [["clojars" {:url "https://clojars.org/repo/"
                                                 :password :gpg}]]
               :jar-exclusions [#"^\."]
               :jvm-opts ["-XX:+TieredCompilation"]
               :certificates ["clojars.pem"]
               :uberjar-exclusions [#"(?i)^META-INF/[^/]*\.(SF|RSA|DSA)$"]})

(defmacro defproject
  "The project.clj file must either def a project map or call this macro.
  See `lein help sample` to see what arguments it accepts."
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

(defn- de-dupe-repo [[repositories seen?] [id settings]]
  ;; repositories from user profiles can be just credentials, so check :url
  (if (or (seen? id) (not (:url settings)))
    [repositories seen?]
    [(conj repositories [id settings]) (conj seen? id)]))

(defn- mapize-settings [repositories]
  (for [[id settings] repositories]
    [id (if (string? settings) {:url settings} settings)]))

(defn normalize-repos [project]
  ;; TODO: got to be a way to tidy this up
  (let [project (update-in project [:repositories] mapize-settings)
        project (if (:deploy-repositories project)
                  (update-in project [:deploy-repositories] mapize-settings)
                  project)
        project (if (:plugin-repositories project)
                  (update-in project [:plugin-repositories] mapize-settings)
                  project)]
    (assoc project :repositories
           (first (reduce de-dupe-repo
                          (if-not (:omit-default-repositories project)
                            [(:repositories defaults)
                             (set (map first (:repositories defaults)))]
                            [[] #{}]) (:repositories project))))))

(defn- without-version [[id version & other]]
  (-> (apply hash-map other)
      (select-keys [:classifier :extension])
      (assoc :id id)))

(defn- dedupe-step [[deps seen] x]
  (if (seen (without-version x))
    ;; this would be so much cleaner if we could just re-use profile-merge
    ;; logic, but since :dependencies are a vector, the :replace/:displace
    ;; calculations don't apply to nested vectors inside :dependencies.
    (let [[seen-dep] (filter #(= (first %) (first x)) deps)]
      (if (or (:displace (meta seen-dep)) (:replace (meta x)))
        [(assoc deps (.indexOf deps seen-dep) x) seen]
        [deps seen]))
    [(conj deps x) (conj seen (without-version x))]))

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

;; # Profiles: basic merge logic

(def default-profiles
  "Profiles get merged into the project map. The :dev, :provided, and :user
  profiles are active by default."
  (atom {:default [:dev :provided :user :base]
         :base {:resource-paths ["dev-resources"]
                :plugins [['lein-newnew "0.3.5"]]
                :checkout-deps-shares [:source-paths
                                       :resource-paths
                                       :compile-path]}
         :leiningen/test {:injections [hooke-injection]}
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

        ;; TODO: last-wins breaks here
        (and (map? result) (map? latter))
        (merge-with profile-key-merge result latter)

        (and (set? result) (set? latter))
        (set/union latter result)

        (and (coll? result) (coll? latter))
        (concat latter result)

        (= (class result) (class latter)) latter

        :else (doto latter (println "has a type mismatch merging profiles."))))

(defn- apply-profiles [project profiles]
  ;; We reverse because we want profile values to override the project, so we
  ;; need "last wins" in the reduce, but we want the first profile specified by
  ;; the user to take precedence.
  (reduce (partial merge-with profile-key-merge)
          project
          (reverse profiles)))

(defn- lookup-profile
  "Lookup a profile in the given profiles map, warning when the profile doesn't
  exist. Recurse whenever a keyword or vector is found, combining all profiles
  in the vector."
  [profiles profile]
  (cond (keyword? profile)
        (let [result (get profiles profile)]
          (when-not (or result (#{:provided :dev :user :test :production} profile))
            (println "Warning: profile" profile "not found."))
          (lookup-profile profiles result))

        ;; composite profile
        (vector? profile)
        (apply-profiles {} (map (partial lookup-profile profiles) profile))

        :else profile))

(defn- warn-user-repos []
  (when (->> (vals (user/profiles))
             (map (comp second :repositories))
             (apply concat) (some :url))
    (println "WARNING: :repositories detected in user-level profile!")
    (println "See https://github.com/technomancy/leiningen/wiki/Repeatability")))

(alter-var-root #'warn-user-repos memoize)

(defn- project-profiles [project]
  (utils/read-file (io/file (:root project) "profiles.clj")))

(defn- read-profiles
  "Read profiles from a variety of sources.

  We check Leiningen's defaults, the profiles.clj file in ~/.lein/profiles.clj,
  the profiles.clj file in the project root, and the :profiles key from the
  project map."
  [project]
  (warn-user-repos)
  (merge @default-profiles (user/profiles)
         (:profiles project) (project-profiles project)))

;; # Lower-level profile plumbing: loading plugins, hooks, middleware, certs

(defn ensure-dynamic-classloader []
  (let [thread (Thread/currentThread)
        cl (.getContextClassLoader thread)]
    (when-not (pomegranate/modifiable-classloader? cl)
      (.setContextClassLoader thread (DynamicClassLoader. cl)))))

(defn- merge-plugin-repositories [project]
  (if-let [pr (:plugin-repositories project)]
    (if (:omit-default-repositories project)
      (assoc project :repositories pr)
      (update-in project [:repositories] concat pr))
    project))

(defn load-plugins
  ([project key]
     (when (seq (get project key))
       (ensure-dynamic-classloader)
       (classpath/resolve-dependencies key (merge-plugin-repositories project)
                                       :add-classpath? true))
     (doseq [wagon-file (-> (.getContextClassLoader (Thread/currentThread))
                            (.getResources "leiningen/wagons.clj")
                            (enumeration-seq))
             [hint factory] (read-string (slurp wagon-file))]
       (aether/register-wagon-factory! hint (eval factory)))
     project)
  ([project] (load-plugins project :plugins)))

(defn plugin-vars [project type]
  (for [[plugin _ & {:as opts}] (:plugins project)
        :when (get opts type true)]
    (with-meta (symbol (str (name plugin) ".plugin")
                       (name type))
      {:optional true})))

(defn- plugin-hooks [project]
  (plugin-vars project :hooks))

(defn- plugin-middleware [project]
  (plugin-vars project :middleware))

(defn- load-hook [hook-name]
  (if-let [hook (try (utils/require-resolve hook-name)
                     (catch Throwable e
                       (utils/error "problem requiring" hook-name "hook")
                       (throw e)))]
    (try (hook)
         (catch Throwable e
           (utils/error "problem activating" hook-name "hook")
           (throw e)))
    (when-not (:optional (meta hook-name))
      (utils/error "cannot resolve" hook-name "hook"))))

(defn load-hooks [project & [ignore-missing?]]
  (doseq [hook-name (concat (plugin-hooks project) (:hooks project))]
    ;; if hook-name is just a namespace assume hook fn is called activate
    (let [hook-name (if (namespace hook-name)
                       hook-name
                       (symbol (name hook-name) "activate"))]
      (load-hook hook-name)))
  project)

(defn apply-middleware
  ([project]
     (reduce apply-middleware project
             (concat (plugin-middleware project)
                     (:middleware project))))
  ([project middleware-name]
     (if-let [middleware (utils/require-resolve middleware-name)]
       (middleware project)
       (do (when-not (:optional (meta middleware-name))
             (utils/error "cannot resolve" middleware-name "middleware"))
           project))))

(defn load-certificates
  "Load the SSL certificates specified by the project and register
   them for use by Aether."
  [project]
  (let [certs (mapcat ssl/read-certs (:certificates project))
        context (ssl/make-sslcontext (into (ssl/default-trusted-certs) certs))]
    (ssl/register-scheme (ssl/https-scheme context))
    project))

(defn activate-middleware
  "A helper funtction to apply middleware and then load certificates and hooks,
  since we always do these three things together, at least so far."
  [project]
  (doto (apply-middleware project)
    (load-certificates)
    (load-hooks)))

(defn ^:internal init-profiles
  "Compute a fresh version of the project map, including and excluding the
  specified profiles."
  [project include-profiles & [exclude-profiles]]
  (let [without-profiles (:without-profiles (meta project) project)
        profile-map (apply dissoc (read-profiles project) exclude-profiles)
        profiles (map (partial lookup-profile profile-map) include-profiles)]
    (-> without-profiles
        (apply-profiles profiles)
        (normalize)
        (vary-meta merge {:without-profiles without-profiles
                          :included-profiles include-profiles
                          :excluded-profiles exclude-profiles}))))

;; # High-level profile operations

(defn set-profiles
  "Compute a fresh version of the project map, with "
  [project include-profiles & [exclude-profiles]]
  (-> project
      (init-profiles include-profiles exclude-profiles)
      (activate-middleware)))

(defn merge-profiles
  "Compute a fresh version of the project map with the given profiles merged
   into list of active profiles and the appropriate middleware applied."
  [project profiles]
  (let [{:keys [included-profiles excluded-profiles]} (meta project)]
    (set-profiles project
      (concat included-profiles profiles)
      (remove (set profiles) excluded-profiles))))

(defn unmerge-profiles
  "Compute a fresh version of the project map with the given profiles unmerged
   from list of active profiles and the appropriate middleware applied."
  [project profiles]
  (let [{:keys [included-profiles excluded-profiles]} (meta project)]
    (set-profiles project
      (remove (set profiles) included-profiles)
      (concat excluded-profiles profiles))))

(defn- init-lein-classpath
  "Adds dependencies to Leiningen's classpath if required."
  [project]
  (when (= :leiningen (:eval-in project))
    (doseq [path (classpath/get-classpath project)]
      (pomegranate/add-classpath path))))

(defn init-project
  "Initializes a project. This is called at startup with the default profiles."
  [project]
  (-> project
      (doto
        (load-certificates)
        (init-lein-classpath)
        (load-plugins))
      (activate-middleware)))

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

(defn read
  "Read project map out of file, which defaults to project.clj."
  ([file profiles]
     (locking read
       (binding [*ns* (find-ns 'leiningen.core.project)]
         (try (load-file file)
              (catch Exception e
                (throw (Exception. "Error loading project.clj" e)))))
       (let [project (resolve 'leiningen.core.project/project)]
         (when-not project
           (throw (Exception. "project.clj must define project map.")))
         ;; return it to original state
         (ns-unmap 'leiningen.core.project 'project)
         (init-profiles (into {} @project) profiles))))
  ([file] (read file [:default]))
  ([] (read "project.clj")))
