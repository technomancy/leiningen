(ns leiningen.core.project
  "Read project.clj files."
  (:refer-clojure :exclude [read])
  (:require [clojure.walk :as walk]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [cemerick.pomegranate :as pomegranate]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.utils :as utils]
            [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath]
            [clojure.string :as str])
  (:import (clojure.lang DynamicClassLoader)
           (java.io PushbackReader Reader)))

(defn make-project-properties [project]
  (with-open [baos (java.io.ByteArrayOutputStream.)]
    (let [properties (doto (java.util.Properties.)
                       (.setProperty "version" (:version project))
                       (.setProperty "groupId" (:group project))
                       (.setProperty "artifactId" (:name project)))
          git-head (utils/resolve-git-dir project)]
      (when (.exists git-head)
        (if-let [revision (utils/read-git-head git-head)]
          (.setProperty properties "revision" revision)))
      (.store properties baos "Leiningen"))
    (str baos)))

(defn- warn [& args]
  ;; TODO: remove with 3.0.0
  (require 'leiningen.core.main)
  (apply (resolve 'leiningen.core.main/warn) args))

(def ^:private warn-once (memoize warn))

(defn- update-each-contained [m keys f & args]
  (reduce (fn [m k]
            (if (contains? m k)
              (apply update m k f args)
              m)) m keys))

(defn- update-first [coll pred f]
  (let [[pre [existing & post]] (split-with (complement pred) coll)]
    (concat pre [(f existing)] post)))

;; # Project definition and normalization

(defn composite-profile?
  "Returns true if the profile is composite, false otherwise."
  [profile]
  (vector? profile))

(defn group-id
  [id]
  (if (string? id)
    (first (str/split id #"/"))
    (or (namespace id) (name id))))

(defn artifact-id
  [id]
  (if (string? id)
    (last (str/split id #"/"))
    (name id)))

(defn artifact-map
  [id]
  {:artifact-id (artifact-id id)
   :group-id (group-id id)})

(defn exclusion-map
  "Transform an exclusion vector into a map that is easier to combine with
  meta-merge. This allows a profile to override specific exclusion options."
  [spec]
  (if-let [[id & {:as opts}] (if (symbol? spec) [spec] spec)]
    (-> opts
        (merge (artifact-map id))
        (with-meta (meta spec)))))

(defn exclusion-vec
  "Transform an exclusion map back into a vector of the form:
  [name/group & opts]"
  [exclusion]
  (if-let [{:keys [artifact-id group-id]} exclusion]
    (into [(symbol group-id artifact-id)]
          (apply concat (dissoc exclusion :artifact-id :group-id)))))

(defn dependency-map
  "Transform a dependency vector into a map that is easier to combine with
  meta-merge. This allows a profile to override specific dependency options."
  [dep]
  (if-let [[id version & {:as opts}] (classpath/normalize-dep-vector dep)]
    (-> opts
        (merge (artifact-map id))
        (assoc :version version)
        (update-each-contained [:exclusions] (partial map exclusion-map))
        (with-meta (meta dep)))))

(defn dependency-vec
  "Transform a dependency map back into a vector of the form:
  [name/group \"version\" & opts]"
  [dep]
  (if-let [{:keys [artifact-id group-id version]} dep]
    (-> dep
        (update-each-contained [:exclusions] (partial map exclusion-vec))
        (update-each-contained [:exclusions] distinct)
        (dissoc :artifact-id :group-id :version)
        (->> (apply concat)
             (into [(symbol group-id artifact-id) version]))
        (with-meta (meta dep)))))

(defn- meta*
  "Returns the metadata of an object, or nil if the object cannot hold
  metadata."
  [obj]
  (if (instance? clojure.lang.IObj obj)
    (meta obj)
    nil))

(defn- with-meta*
  "Returns an object of the same type and value as obj, with map m as its
  metadata if the object can hold metadata."
  [obj m]
  (if (instance? clojure.lang.IObj obj)
    (with-meta obj m)
    obj))

(defn- displace?
  "Returns true if the object is marked as displaceable"
  [obj]
  (-> obj meta* :displace))

(defn- replace?
  "Returns true if the object is marked as replaceable"
  [obj]
  (-> obj meta* :replace))

(defn- top-displace?
  "Returns true if the object is marked as top-displaceable"
  [obj]
  (-> obj meta* :top-displace))

(defn- different-priority?
  "Returns true if either left has a higher priority than right or vice versa."
  [left right]
  (boolean
   (or (some (some-fn nil? displace? replace?) [left right])
       (top-displace? left))))

(defn- remove-top-displace [obj]
  (if-not (top-displace? obj)
    obj
    (vary-meta obj dissoc :top-displace)))

(defn- pick-prioritized
  "Picks the highest prioritized element of left and right and merge their
  metadata."
  [left right]
  (cond (nil? left) right
        (nil? right) (remove-top-displace left)

        ;; TODO: support :reverse?
        (top-displace? left) right
        (and (displace? left) (top-displace? right)) left

        (and (displace? left)   ;; Pick the rightmost
             (displace? right)) ;; if both are marked as displaceable
        (with-meta* right
          (merge (meta* left) (meta* right)))

        (and (replace? left)    ;; Pick the rightmost
             (replace? right))  ;; if both are marked as replaceable
        (with-meta* right
          (merge (meta* left) (meta* right)))

        (or (displace? left)
            (replace? right))
        (with-meta* right
          (merge (-> left meta* (dissoc :displace))
                 (-> right meta* (dissoc :replace))))

        (or (replace? left)
            (displace? right))
        (with-meta* left
          (merge (-> right meta* (dissoc :displace))
                 (-> left meta* (dissoc :replace))))))

(declare meta-merge)

;; TODO: drop this and use read-eval syntax in 3.0
(defn- unquote-project
  "Inside defproject forms, unquoting (~) allows for arbitrary evaluation."
  [args]
  (walk/walk (fn [item]
               (cond (and (seq? item) (= `unquote (first item))) (second item)
                     ;; needed if we want fn literals preserved
                     (or (seq? item) (symbol? item)) (list 'quote item)
                     :else (let [result (unquote-project item)]
                             ;; clojure.walk strips metadata
                             (if-let [m (meta item)]
                               (with-meta result m)
                               result))))
             identity
             args))

(def defaults
  ;; TODO: move :repositories here in 3.0
  {:source-paths ^:top-displace ^:default-path/src ["src"]
   :resource-paths ^:top-displace ^:default-path/resources ["resources"]
   :test-paths ^:top-displace ^:default-path/test ["test"]
   :native-path "%s/native"
   :compile-path "%s/classes"
   :target-path "target"
   :clean-targets ^:top-displace [:target-path]
   ;; TODO: remove :top-displace for :prep-tasks in 3.0
   :prep-tasks ^:top-displace ["javac" "compile"]
   ;; If these change, be sure to update release docstring and DEPLOY.md
   :release-tasks ^:top-displace [["vcs" "assert-committed"]
                                  ["change" "version"
                                   "leiningen.release/bump-version" "release"]
                                  ["vcs" "commit"]
                                  ["vcs" "tag"]
                                  ["deploy"]
                                  ["change" "version"
                                   "leiningen.release/bump-version"]
                                  ["vcs" "commit"]
                                  ["vcs" "push"]]
   :pedantic? (quote ^:top-displace ranges)
   :jar-exclusions [#"^\."]
   :eval-in :default
   :offline? (not (nil? (System/getenv "LEIN_OFFLINE")))
   :uberjar-exclusions [#"(?i)^META-INF/[^/]*\.(SF|RSA|DSA)$"]
   :uberjar-merge-with {"META-INF/plexus/components.xml"
                        'leiningen.uberjar/components-merger,
                        "data_readers.clj"
                        'leiningen.uberjar/clj-map-merger,
                        ;; So we don't break Java's ServiceLoader mechanism
                        ;; during uberjar construction
                        #"META-INF/services/.*" `[slurp #(str %1 "\n" %2) spit]}
   :global-vars {}})

(defn dep-key
  "The unique key used to dedupe dependencies."
  [dep]
  (-> (dependency-map dep)
      (select-keys [:group-id :artifact-id :classifier :extension])))

(defn- reduce-dep-step [deps dep]
  (let [k (dep-key dep)]
    (update-first deps #(= k (dep-key %))
                  (fn [existing]
                    (dependency-vec
                     (meta-merge (dependency-map existing)
                                 (dependency-map dep)))))))

(defn normalize-aot [project]
  (if (= :all (:aot project))
    (assoc project :aot ^:replace [:all])
    project))

(defn- normalize-repo
  "Normalizes a repository to the canonical repository form."
  [[id opts :as repo]]
  (with-meta
    [id (if (string? opts) {:url opts} opts)]
    (meta repo)))

(defn- normalize-repos
  "Normalizes a vector of repositories to the canonical repository form."
  [repos]
  (with-meta
    (mapv normalize-repo repos)
    (meta repos)))

(defn- reduce-repo-step [repos [id opts :as repo]]
  (update-first repos #(= id (first %))
                (fn [[_ existing :as original]]
                  (let [opts (if (keyword? opts)
                               (-> (filter #(= (first %) (name opts)) repos)
                                   first second)
                               opts)
                        repo (with-meta [id opts] (meta repo))]
                    (if (different-priority? repo original)
                      (pick-prioritized repo original)
                      (with-meta [id (meta-merge existing opts)]
                        (merge (meta original) (meta repo))))))))

(def empty-dependencies
  (with-meta [] {:reduce reduce-dep-step}))

(def empty-repositories
  (with-meta [] {:reduce reduce-repo-step}))

(def empty-paths
  (with-meta [] {:prepend true}))

(def default-repositories
  (with-meta
    [["central" {:url "https://repo1.maven.org/maven2/" :snapshots false}]
     ["clojars" {:url "https://repo.clojars.org/"}]]
    {:reduce reduce-repo-step}))

(def deploy-repositories
  (with-meta
    [["clojars" {:url "https://clojars.org/repo/"
                 :password :gpg :username :gpg}]]
    {:reduce reduce-repo-step}))

(defn normalize-values
  "Transform values within a project or profile map to normalized values, such
  that internal functions can assume that the values are already normalized."
  [map]
  (-> map
      (update-each-contained [:repositories :deploy-repositories
                              :mirrors :plugin-repositories] normalize-repos)
      (update-each-contained [:profiles] utils/map-vals normalize-values)
      (normalize-aot)))

(def ^:private empty-meta-merge-defaults
  {:repositories empty-repositories
   :plugin-repositories empty-repositories
   :deploy-repositories deploy-repositories
   :plugins empty-dependencies
   :dependencies empty-dependencies
   :source-paths empty-paths
   :resource-paths empty-paths
   :test-paths empty-paths})

(defn- setup-map-defaults
  "Transform a project or profile map by merging empty default values containing
  reducing functions and other metadata properties, replacing aliases and
  normalizing values inside the map."
  [raw-map empty-defaults]
  (with-meta
    (merge-with
     (fn [left right]
       ;; Assumes that left always contains :reduce OR :prepend in its meta
       (with-meta
         (cond (different-priority? left right) (pick-prioritized left right)
               (-> left meta :reduce) (-> left meta :reduce
                                          (reduce left right))
               (-> left meta :prepend) (concat right left))
         (merge (meta left)
                (dissoc (meta right) :top-displace))))
     empty-defaults
     (-> raw-map
         (assoc :jvm-opts (or (:jvm-opts raw-map) (:java-opts raw-map)))
         (assoc :eval-in (or (:eval-in raw-map)
                             (if (:eval-in-leiningen raw-map)
                               :leiningen)))
         (dissoc :eval-in-leiningen :java-opts)
         (normalize-values)))
    (meta raw-map)))

(defn- with-normalized-deps
  [profile]
  (let [deps (:dependencies profile)]
    (assoc profile
      :dependencies
      (with-meta
       (classpath/normalize-dep-vectors deps)
       (meta deps)))))

(defn- setup-profile-with-empty
  "Setup a profile map with empty defaults."
  [raw-profile]
  (if (composite-profile? raw-profile)
    ;; TODO: drop support for partially-composite profiles in 3.0
    (with-meta
      (mapv #(cond-> % (composite-profile? %) setup-profile-with-empty)
            raw-profile)
      (meta raw-profile))
    (let [empty-defaults (select-keys empty-meta-merge-defaults
                                      (keys raw-profile))]
      (setup-map-defaults
       (with-normalized-deps raw-profile)
       empty-defaults))))

(defn- setup-map-of-profiles
  "Setup a map of profile maps with empty defaults."
  [map-of-profiles]
  (utils/map-vals map-of-profiles setup-profile-with-empty))

(defn make
  ([project project-name version root]
     (make (with-meta (assoc project
                        :name (name project-name)
                        :group (or (namespace project-name)
                                   (name project-name))
                        :version version
                        :root root)
             (meta project))))
  ([project]
     (let [repos (if (:omit-default-repositories project)
                   (do (warn-once "WARNING:"
                                  ":omit-default-repositories is deprecated;"
                                  "use :repositories ^:replace [...] instead.")
                       empty-repositories)
                   default-repositories)]
       (setup-map-defaults
        (-> (meta-merge defaults project)
            (dissoc :eval-in-leiningen :omit-default-repositories)
            (assoc :eval-in (or (:eval-in project)
                                (if (:eval-in-leiningen project)
                                  :leiningen, :subprocess)))
            (update-each-contained [:profiles] setup-map-of-profiles)
            (with-meta (meta project)))
        (assoc empty-meta-merge-defaults
          :repositories repos
          :plugin-repositories repos)))))

(defn- argument-list->argument-map
  [args]
  (let [keys (map first (partition 2 args))
        unique-keys (set keys)]
    (if (= (count keys) (count unique-keys))
      (apply hash-map args)
      (let [duplicates (->> (frequencies keys)
                            (remove #(> 2 (val %)))
                            (map first))]
        (throw
         (IllegalArgumentException.
          (format "Duplicate keys: %s"
                  (clojure.string/join ", " duplicates))))))))

(defmacro defproject
  "The project.clj file must either def a project map or call this macro.
  See `lein help sample` to see what arguments it accepts."
  [project-name version & args]
  (let [f (io/file *file*)]
    `(let [args# ~(unquote-project (argument-list->argument-map args))
           root# ~(if f (.getParent f))]
       (def ~'project
         (make args# '~project-name ~version root#)))))

(defn- add-exclusions [exclusions dep]
  (dependency-vec
   (update-in (dependency-map dep) [:exclusions]
              into (map exclusion-map exclusions))))

(defn- add-global-exclusions [project]
  (let [{:keys [dependencies exclusions]} project]
    (if-let [exclusions (and (seq dependencies) (seq exclusions))]
      (assoc project
        :dependencies (with-meta
                        (mapv (partial add-exclusions exclusions)
                              dependencies)
                        (meta dependencies)))
      project)))

(defn- absolutize [root path]
  (str (if (.isAbsolute (io/file path))
         path
         (io/file root path))))

(defn- absolutize-path [{:keys [root] :as project} key]
  (cond (re-find #"-path$" (name key))
        (update project key (partial absolutize root))

        (re-find #"-paths$" (name key))
        (update project key #(with-meta* (map (partial absolutize root) %)
                               (meta %)))

        :else project))

(defn absolutize-paths [project]
  (reduce absolutize-path project (keys project)))

(defn- sha1 [content]
  (.toString (BigInteger. 1 (-> (java.security.MessageDigest/getInstance "SHA1")
                                (.digest (.getBytes content)))) 16))

(defn- keyword-composite-profile? [profile]
  (and (composite-profile? profile) (every? keyword? profile)))

(defn- ordered-keyword-composite-profiles [project]
  (->> project meta :profiles
       (filter (comp keyword-composite-profile? val))
       (remove (comp empty? val))
       (sort-by count)
       (reverse)))

(defn- first-matching-composite [profiles composites]
  (->> composites
       (filter (fn [[_ v]] (= v (take (count v) profiles))))
       (first)))

(defn- normalize-profile-names [project profiles]
  (let [composites (ordered-keyword-composite-profiles project)]
    (loop [profiles'  profiles
           normalized ()]
      (if (seq profiles')
        (if-let [[k v] (first-matching-composite profiles' composites)]
          (recur (drop (count v) profiles') (cons k normalized))
          (recur (rest profiles') (cons (first profiles') normalized)))
        (if (= (count profiles) (count normalized))
          profiles
          (normalize-profile-names project (reverse normalized)))))))

(defn profile-scope-target-path [project profiles]
  (let [n #(if (map? %) (subs (sha1 (pr-str %)) 0 8) (name %))]
    (if (:target-path project)
      (update-in project [:target-path] format
                 (str/join "+" (map n (normalize-profile-names project profiles))))
      project)))

(defn target-path-subdirs [{:keys [target-path] :as project} key]
  (if (project key)
    (update-in project [key] format target-path)
    project))

;; # Profiles: basic merge logic

(def ^:private hooke-injection
  (with-open [rdr (-> "robert/hooke.clj" io/resource io/reader PushbackReader.)]
    `(do (ns ~'leiningen.core.injected)
         ~@(doall (take-while #(not= % ::eof)
                              (rest (repeatedly #(clojure.core/read
                                                  rdr false ::eof)))))
         (ns ~'user))))

;; users of old JVMs will have to set LEIN_JVM_OPTS to turn off tiered
;; compilation, so if they've done that we should do the same for project JVMs
(def tiered-jvm-opts
  (if (.contains (or (System/getenv "LEIN_JVM_OPTS") "") "Tiered")
    ["-XX:+TieredCompilation" "-XX:TieredStopAtLevel=1"]))

(def default-jvm-opts
  [;; actually does the opposite; omits trace unless this is set
   "-XX:-OmitStackTraceInFastThrow"])

(def default-profiles
  "Profiles get merged into the project map. The :dev, :provided, and :user
  profiles are active by default."
  (atom {:default [:leiningen/default]
         :leiningen/default [:base :system :user :provided :dev]
         :base {:resource-paths ^:default-path/dev-resources ["dev-resources"]
                :jvm-opts (with-meta `[~@default-jvm-opts
                                       ~@tiered-jvm-opts]
                            {:displace true})
                :test-selectors {:default (with-meta '(constantly true)
                                            {:displace true})}
                ;; bump deps in leiningen's own project.clj with these
                :dependencies '[^:displace [org.clojure/tools.nrepl "0.2.12"
                                            :exclusions [org.clojure/clojure]]
                                ^:displace [clojure-complete "0.2.5"
                                            :exclusions [org.clojure/clojure]]]
                :checkout-deps-shares [:source-paths
                                       :test-paths
                                       :resource-paths
                                       :compile-path
                                       #'classpath/checkout-deps-paths]
                :aliases {"downgrade" "upgrade"}}
         :leiningen/test {:injections [hooke-injection]
                          :test-selectors {:default (with-meta
                                                      '(constantly true)
                                                      {:displace true})}}
         :uberjar {} ; TODO: use :aot :all here in 3.0
         :update {:update :always}
         :offline {:offline? true}
         :debug {:debug true}}))

(def default-profile-metadata
  {:dev {:pom-scope :test}
   :test {:pom-scope :test}
   :uberjar {:leaky true}
   :provided {:pom-scope :provided}
   :repl {:repl true}})

(defn- meta-merge
  "Recursively merge values based on the information in their metadata."
  [left right]
  (cond (different-priority? left right)
        (pick-prioritized left right)

        (-> left meta :reduce)
        (-> left meta :reduce
            (reduce left right)
            (with-meta (meta left)))

        (and (map? left) (map? right))
        (merge-with meta-merge left right)

        (and (set? left) (set? right))
        (set/union right left)

        (and (coll? left) (coll? right))
        (if (or (-> left meta :prepend)
                (-> right meta :prepend))
          (-> (concat right left)
              (with-meta (merge (meta right) (meta left))))
          (concat left right))

        (= (class left) (class right)) right

        :else
        (do (warn left "and" right "have a type mismatch merging profiles.")
            right)))

(defn- apply-profiles [project profiles]
  (reduce (fn [project profile]
            (with-meta
              (meta-merge project profile)
              (meta-merge (meta project) (meta profile))))
          project
          profiles))

(defn- lookup-profile*
  "Lookup a profile in the given profiles map, warning when the profile doesn't
  exist. Recurse whenever a keyword or vector is found, combining all profiles
  in the vector."
  [profiles profile]
  (cond (keyword? profile)
        (let [result (get profiles profile)]
          (when-not (or result (#{:provided :dev :user :test :base :default
                                  :production :system :repl}
                                profile))
            (warn "Warning: profile" profile "not found."))
          (lookup-profile* profiles result))

        (composite-profile? profile)
        (apply-profiles {} (map (partial lookup-profile* profiles) profile))

        :else (or profile {})))

(defn- lookup-profile
  "Equivalent with lookup-profile*, except that it will attach the profile name
  as an active profile in the profile metadata if the profile is a keyword."
  [profiles profile]
  (cond-> (lookup-profile* profiles profile)
          (keyword? profile)
          (vary-meta update-in [:active-profiles] (fnil conj []) profile)))

(defn- expand-profile* [profiles profile-meta profile]
  (let [content (or (get profiles profile) (get @default-profiles profile))]
    ;; TODO: drop "support" for partially-composite profiles in 3.0
    (if (or (nil? content)
            (map? content)
            (and (sequential? content)
                 (some map? content)))
      [[profile profile-meta]]
      (mapcat (partial expand-profile*
                       profiles (merge profile-meta (meta content)))
              (if (sequential? content)
                content
                [content])))))

(defn expand-profile-with-meta
  "Recursively expand the keyword `profile` in `project` to a sequence of
  vectors of atomic (non-composite) profile keywords and their inherited
  metadata."
  [project profile]
  (expand-profile* (:profiles (meta project)) nil profile))

(defn expand-profiles-with-meta
  "Recursively expand a collection of profiles"
  [project profiles]
  (mapcat (partial expand-profile-with-meta project) profiles))

(defn expand-profile
  "Recursively expand the keyword `profile` in `project` to a sequence of
  atomic (non-composite) profile keywords."
  [project profile]
  (->> (expand-profile* (:profiles (meta project)) nil profile)
       (map first)))

(defn expand-profiles
  "Recursively expand a collection of profiles"
  [project profiles]
  (mapcat (partial expand-profile project) profiles))

(defn- warn-user-repos [profiles]
  (let [has-url? (fn [entry] (or (string? entry) (:url entry)))
        repo-profiles (filter #(->> (second %)
                                    :repositories
                                    (map second)
                                    (some has-url?))
                              profiles)]
    (when (and (seq repo-profiles)
               (not (System/getenv "LEIN_SUPPRESS_USER_LEVEL_REPO_WARNINGS")))
      (warn-once ":repositories detected in user-level profiles!"
                 (vec (map first repo-profiles)) "\nSee"
                 "https://github.com/technomancy/leiningen/wiki/Repeatability"))))

(defn- warn-user-profile [root profiles]
  (when (and root (contains? profiles :user))
    (warn-once "WARNING: user-level profile defined in project files.")))

(defn- system-profiles []
  (let [sys-profile-dir (if (= :windows (utils/get-os))
                          (io/file (System/getenv "AllUsersProfile") "Leiningen")
                          (io/file "/etc" "leiningen"))]
    (user/load-profiles sys-profile-dir)))

(defn ^:internal project-profiles [project]
  (let [profiles (utils/read-file (io/file (:root project) "profiles.clj"))]
    (warn-user-profile (:root project) profiles)
    profiles))

(defn read-profiles
  "Read profiles from a variety of sources.

  We check Leiningen's defaults, system-level profiles (usually in
  /etc), the profiles.clj file in ~/.lein, the profiles.clj file in
  the project root, and the :profiles key from the project map."
  [project]
  ;; TODO: All profile reads (load-profiles and profiles, notable) should wrap
  ;;   setup-profiles instead of doing stuff here, but as it is a cyclic
  ;;   dependency, defer it to 3.0. Although I guess we don't need this
  ;;   functionality for 3.0 if we're smart.
  (let [sys-profiles (setup-map-of-profiles (system-profiles))
        user-profiles (setup-map-of-profiles (user/profiles))
        proj-profiles-file (setup-map-of-profiles (project-profiles project))]
    (warn-user-repos (concat user-profiles sys-profiles))
    (warn-user-profile (:root project) (:profiles project))
    (merge @default-profiles sys-profiles user-profiles
           (:profiles project) proj-profiles-file)))

(defn- scope-plugin-profile [local-name plugin-name]
  (keyword (str "plugin." plugin-name) (name local-name)))

(defn- read-plugin-profiles [project]
  (let [p (for [[plugin version] (:plugins project)
                :let [profiles (io/resource (format "%s/profiles.clj"
                                                    (name plugin)))]
                :when profiles]
            (for [[local-name profile] (read-string (slurp profiles))]
              [(scope-plugin-profile local-name (name plugin)) profile]))]
    (into {} (apply concat p))))

;; # Lower-level profile plumbing: loading plugins, hooks, middleware, certs

(defn ensure-dynamic-classloader []
  (let [thread (Thread/currentThread)
        cl (.getContextClassLoader thread)]
    (when-not (instance? DynamicClassLoader cl)
      (.setContextClassLoader thread (DynamicClassLoader. cl)))))

(def ^:private registered-wagon-files (atom #{}))

(defn load-plugins
  ([project dependencies-key managed-dependencies-key]
     (when (seq (get project dependencies-key))
       (let [repos-project (update-in project [:repositories] meta-merge
                                      (:plugin-repositories project))]
         (classpath/resolve-managed-dependencies
          dependencies-key managed-dependencies-key repos-project
          :add-classpath? true)))
     (doseq [wagon-file (-> (.getContextClassLoader (Thread/currentThread))
                            (.getResources "leiningen/wagons.clj")
                            (enumeration-seq))
             :when (not (@registered-wagon-files wagon-file))
             [hint factory] (read-string (slurp wagon-file))]
       (aether/register-wagon-factory! hint (eval factory))
       (swap! registered-wagon-files conj wagon-file))
     project)
  ([project dependencies-key] (load-plugins project dependencies-key nil))
  ([project] (load-plugins project :plugins)))

(defn plugin-vars [project type]
  (for [[plugin _ & {:as opts}] (:plugins project)
        :when (get opts type true)]
    (-> (symbol (str (name plugin) ".plugin") (name type))
        (with-meta {:optional true}))))

(defn- plugin-hooks [project]
  (plugin-vars project :hooks))

(defn- plugin-middleware [project]
  (plugin-vars project :middleware))

(defn- load-hook [hook-name]
  (if-let [hook (try (utils/require-resolve hook-name)
                     (catch Exception e
                       (utils/error "problem requiring" hook-name "hook")
                       (throw e)))]
    (try (warn-once "Warning: implicit hook found:" hook-name
                    "\nHooks are deprecated and will be removed"
                    "in a future version.")
         (hook)
         (catch Exception e
           (utils/error "problem activating" hook-name "hook")
           (throw e)))
    (when-not (:optional (meta hook-name))
      (utils/error "cannot resolve" hook-name "hook"))))

(defn load-hooks [project & [ignore-missing?]]
  (when (and (:implicits project true) (:implicit-hooks project true))
    (doseq [hook-name (concat (plugin-hooks project) (:hooks project))]
      ;; if hook-name is just a namespace assume hook fn is called activate
      (let [hook-name (if (namespace hook-name)
                        hook-name
                        (symbol (name hook-name) "activate"))]
        (load-hook hook-name))))
  project)

(defn apply-middleware
  ([project]
     (reduce apply-middleware project
             (concat (plugin-middleware project) (:middleware project))))
  ([project middleware-name]
     (if (and (:implicits project true) (:implicit-middleware project true))
       (if-let [middleware (utils/require-resolve middleware-name)]
         (do (when-not (some #{middleware-name} (:middleware project))
               (warn-once "Warning: implicit middleware found:" middleware-name
                          "\nPlease declare all middleware in :middleware"
                          "as implicit loading is deprecated."))
             (middleware project))
         (do (when-not (:optional (meta middleware-name))
               (utils/error "cannot resolve" middleware-name "middleware"))
             project))
       project)))

(defn load-certificates
  "Load the SSL certificates specified by the project and register
   them for use by Aether."
  [project]
  (when (seq (:certificates project))
    ;; lazy-loading might give a launch speed boost here
    (require 'leiningen.core.ssl)
    (let [make-context (resolve 'leiningen.core.ssl/make-sslcontext)
          read-certs (resolve 'leiningen.core.ssl/read-certs)
          default-certs (resolve 'leiningen.core.ssl/default-trusted-certs)
          override-wagon-registry! (resolve 'leiningen.core.ssl/override-wagon-registry!)
          https-registry (resolve 'leiningen.core.ssl/https-registry)
          certs (mapcat read-certs (:certificates project))
          context (make-context (into (default-certs) certs))]
      (override-wagon-registry! (https-registry context))
      project)))

(defn activate-middleware
  "A helper function to apply middleware and then load certificates and hooks,
  since we always do these three things together, at least so far."
  [project]
  (doto (apply-middleware project)
    (load-certificates)
    (load-hooks)))

(defn project-with-profiles-meta [project profiles]
  ;;; should this dissoc :default?
  ;; (vary-meta project assoc :profiles (dissoc profiles :default))
  (vary-meta project assoc
             :profiles profiles))

(defn- apply-profile-meta [default-meta profile]
  (if (map? profile)
    (let [profile (vary-meta profile (fn [m] (merge default-meta m)))]
      (if-let [scope (:pom-scope (meta profile))]
        (with-meta
          (update-in profile [:dependencies]
                     (fn [deps]
                       (map
                        (fn [dep]
                          (if (some #(= :scope %) dep)
                            dep
                            (-> dep (conj :scope) (conj (name scope)))))
                        deps)))
          (meta profile))
        profile))
    profile))

(defn project-with-profiles [project]
  (let [profiles (merge (read-plugin-profiles project)
                        (read-profiles project))]
    (project-with-profiles-meta
     project
     (->> (map (fn [[k p]]
                 [k (apply-profile-meta (default-profile-metadata k) p)])
               profiles)
          (into {})))))

(defn ^:internal init-profiles
  "Compute a fresh version of the project map, including and excluding the
  specified profiles."
  [project include-profiles & [exclude-profiles]]
  (let [project (with-meta
                  (:without-profiles (meta project) project)
                  (meta project))
        include-profiles-meta (->> (expand-profiles-with-meta
                                    project include-profiles)
                                   (utils/last-distinct-by first))
        include-profiles (map first include-profiles-meta)
        exclude-profiles (utils/last-distinct (expand-profiles project exclude-profiles))
        normalize #(if (coll? %) (lookup-profile (:profiles project) %) [%])
        exclude-profiles (mapcat normalize exclude-profiles)
        profile-map (apply dissoc (:profiles (meta project)) exclude-profiles)
        profiles (map (partial lookup-profile profile-map) include-profiles)
        normalized-profiles (map normalize-values profiles)]
    (-> project
        (apply-profiles normalized-profiles)
        (profile-scope-target-path include-profiles)
        (target-path-subdirs :compile-path)
        (target-path-subdirs :native-path)
        (absolutize-paths)
        (add-global-exclusions)
        (vary-meta merge {:without-profiles project
                          :included-profiles include-profiles
                          :excluded-profiles exclude-profiles
                          :profile-inherited-meta include-profiles-meta}))))

(def whitelist-keys
  "Project keys which don't affect the production of the jar (sans its name)
  should be propagated to the compilation phase and not stripped out."
  [:certificates :jar-name :local-repo :mirrors :offline? :repositories :uberjar-name :warn-on-reflection])

(defn retain-whitelisted-keys
  "Retains the whitelisted keys from the original map in the new one."
  [new original]
  (merge new (select-keys original whitelist-keys)))

;; # High-level profile operations

(defn set-profiles
  "Compute a fresh version of the project map, with middleware applied,
  including and excluding the specified profiles."
  [project include-profiles & [exclude-profiles]]
  (-> project
      (init-profiles include-profiles exclude-profiles)
      (load-plugins)
      (activate-middleware)))

(defn merge-profiles
  "Compute a fresh version of the project map with the given profiles merged
  into list of active profiles and the appropriate middleware applied."
  [project profiles]
  (let [{:keys [included-profiles excluded-profiles]} (meta project)
        profiles (expand-profiles project profiles)]
    (set-profiles project
                  (concat included-profiles profiles)
                  (remove (set profiles) excluded-profiles))))

(defn unmerge-profiles
  "Compute a fresh version of the project map with the given profiles unmerged
  from list of active profiles and the appropriate middleware applied."
  [project profiles]
  (let [{:keys [included-profiles excluded-profiles]} (meta project)
        profiles (expand-profiles project profiles)]
    (set-profiles project
                  (remove (set profiles) included-profiles)
                  (concat excluded-profiles profiles))))

(defn init-lein-classpath
  "Adds dependencies to Leiningen's classpath if required."
  [project]
  (when (= :leiningen (:eval-in project))
    (doseq [path (classpath/get-classpath project)]
      (pomegranate/add-classpath path))))

(defn init-project
  "Initializes a project by loading certificates, plugins, middleware, etc.
Also merges default profiles."
  ([project default-profiles]
     (-> (project-with-profiles (doto project
                                  (load-certificates)
                                  (init-lein-classpath)
                                  (load-plugins)))
         (init-profiles default-profiles)
         (load-plugins)
         (activate-middleware)))
  ([project] (init-project project [:default])))

(defn add-profiles
  "Add the profiles in the given profiles map to the project map, taking care
  to preserve project map metadata. Note that these profiles are not merged,
  merely made available to merge by name."
  [project profiles-map]
  (-> (update-in project [:profiles] merge profiles-map)
      (vary-meta merge
                 {:without-profiles
                  (update-in (:without-profiles (meta project) project)
                             [:profiles]
                             merge profiles-map)})
      (vary-meta update-in [:profiles] merge profiles-map)))

(defn profile-annotations
  "Return a map of profile keyword to profile annotations for the profiles
  in :include-profiles."
  [project]
  (->> (map
        (juxt first (fn [[profile m]]
                      (merge m (meta ((-> project meta :profiles) profile)))))
        (-> project meta :profile-inherited-meta))
       (into {})))

(defn profiles-with-matching-meta
  "Return a sequence of profile keywords for the project profiles that
  have metadata that satisfies the predicate, pred."
  [project pred]
  (->> (-> project meta :profiles)
       (filter (comp pred meta val))
       (map key)))

(defn non-leaky-profiles
  "Return a sequence of profile keywords for the non-leaky profiles
  currently included in the project."
  [project]
  (->> (profile-annotations project)
       (remove (comp :leaky val))
       (map key)))

(defn pom-scope-profiles
  "Return a sequence of profile keywords for the currently active
  project profiles with :pom-scope equal to scope."
  [project scope]
  (->> (profile-annotations project)
       (filter (comp #(= scope (:pom-scope %)) val))
       keys))

(defn read-raw
  "Read project file without loading certificates, plugins, middleware, etc."
  [source]
  (locking read-raw
    (binding [*ns* (find-ns 'leiningen.core.project)]
      (try
        (if (instance? Reader source)
          (load-reader source)
          (load-file source))
        (catch Exception e
          (throw (Exception. (format "Error loading %s" source) e)))))
    (let [project (resolve 'leiningen.core.project/project)]
      (when-not project
        (throw (Exception. (format "%s must define project map" source))))
      ;; return it to original state
      (ns-unmap 'leiningen.core.project 'project)
      @project)))

(defn read
  "Read project map out of file, which defaults to project.clj.
Also initializes the project; see read-raw for a version that skips init."
  ([file profiles] (init-project (read-raw file) profiles))
  ([file] (read file [:default]))
  ([] (read "project.clj")))

;; Basically just for re-throwing a more comprehensible error.
(defn- read-dependency-project [project-file]
  (if (.exists project-file)
    (let [project (.getAbsolutePath project-file)]
      (try (read project)
           (catch Exception e
             (throw (Exception. (format "Problem loading %s" project) e)))))
    (warn "WARN ignoring checkouts directory" (.getParent project-file)
             "as it does not contain a project.clj file.")))

(alter-var-root #'read-dependency-project memoize)

(defn read-checkouts
  "Returns a list of project maps for this project's checkout
  dependencies."
  [project]
  (for [dep (.listFiles (io/file (:root project) "checkouts"))
        :let [project-file (.getCanonicalFile (io/file dep "project.clj"))
              checkout-project (read-dependency-project project-file)]
        :when checkout-project]
    checkout-project))
