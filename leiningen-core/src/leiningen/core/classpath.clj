(ns leiningen.core.classpath
  "Calculate project classpaths by resolving dependencies via Aether."
  (:require [cemerick.pomegranate.aether :as aether]
            [cemerick.pomegranate :as pomegranate]
            [clojure.java.io :as io]
            [leiningen.core.user :as user])
  (:import (java.util.jar JarFile)
           (java.util.regex Pattern)
           (java.net URL)))

;; Basically just for re-throwing a more comprehensible error.
(defn- read-dependency-project [root dep]
  (let [project-file (io/file root "checkouts" dep "project.clj")]
    (if (.exists project-file)
      (let [project (.getAbsolutePath project-file)]
        ;; TODO: core.project and core.classpath currently rely upon each other *uk*
        (require 'leiningen.core.project)
        (try ((resolve 'leiningen.core.project/read) project [])
             (catch Exception e
               (throw (Exception. (format "Problem loading %s" project) e)))))
      (println
       "WARN ignoring checkouts directory" dep
       "as it does not contain a project.clj file."))))

(defn- checkout-dep-paths [project dep dep-project]
  (if-let [shares (:checkout-deps-shares project)]
    (map #(% dep-project) shares)
    (concat (:source-paths dep-project)
            (:resource-paths dep-project)
            [(:compile-path dep-project)])))

(defn- checkout-deps-paths
  "Checkout dependencies are used to place source for a dependency
  project directly on the classpath rather than having to install the
  dependency and restart the dependent project."
  [project]
  (apply concat (for [dep (.list (io/file (:root project) "checkouts"))
                      :let [dep-project (read-dependency-project
                                         (:root project) dep)]]
                  (checkout-dep-paths project dep dep-project))))

(defn extract-native-deps [deps native-path]
  (doseq [jar (map #(JarFile. %) deps)
          entry (enumeration-seq (.entries jar))
          :when (.startsWith (.getName entry) "native/")]
    (let [f (io/file native-path (subs (.getName entry) (count "native/")))]
      (if (.isDirectory entry)
        (.mkdirs f)
        (io/copy (.getInputStream jar entry) f)))))

(defn add-repo-auth
  "Repository credentials (a map containing some of
   #{:username :password :password :private-key-file}) are discovered
   from:

   1. Looking up the repository URL in the [:auth :repository-auth]
      map
   2. Scanning that map for regular expression keys that match the
      repository URL.

   So, a :repository-auth map that contains an entry:

     {#\"http://maven.company.com/.*\" {:username \"abc\" :password \"xyz\"}}

   would be applied to all repositories with URLs matching the regex key
   that didn't have an explicit entry."
  [[id {:keys [url] :as repo}]]
  (let [repo-creds (-> (user/profiles) :auth :repository-auth)]
    (if-let [match (get repo-creds url)]
      [id (merge repo match)]
      [id (merge repo (first (for [[re? cred] repo-creds
                                   :when (and (instance? Pattern re?)
                                              (re-matches re? url))]
                               cred)))])))

(defn add-auth [repositories]
  (map add-repo-auth repositories))

(defn get-proxy-settings
  "Returns a map of the JVM proxy settings"
  []
  (when-let [proxy (System/getenv "http_proxy")]
    (let [url (URL. proxy)
          user-info (.getUserInfo url)
          [username password] (and user-info (.split user-info ":"))]
      {:host (.getHost url)
       :port (.getPort url)
       :username username
       :password password})))

(defn- root-cause [e]
  (last (take-while identity (iterate (memfn getCause) e))))

(defn- get-dependencies
  [dependencies-key {:keys [repositories local-repo offline?] :as project}
   & {:keys [add-classpath?]}]
  {:pre [(every? vector? (project dependencies-key))]}
  (try
    ((if add-classpath?
       pomegranate/add-dependencies
       aether/resolve-dependencies)
     :local-repo local-repo
     :offline? offline?
     :repositories (add-auth repositories)
     :coordinates (project dependencies-key)
     :transfer-listener :stdout
     :proxy (get-proxy-settings))
    (catch Exception e
      (if (and (instance? java.net.UnknownHostException (root-cause e))
               (not offline?))
        (get-dependencies dependencies-key (assoc project :offline? true))
        (throw e)))))

(defn resolve-dependencies
  "Simply delegate regular dependencies to pomegranate. This will
  ensure they are downloaded into ~/.m2/repositories and that native
  deps have been extracted to :native-path. If :add-classpath? is
  logically true, will add the resolved dependencies to Leiningen's
  classpath.

   Returns a set of the dependencies' files."
  [dependencies-key {:keys [repositories native-path] :as project} & rest]
  (doto (->> (apply get-dependencies dependencies-key project rest)
             (aether/dependency-files)
             (filter #(re-find #"\.(jar|zip)$" (.getName %))))
    (extract-native-deps native-path)))

(defn dependency-hierarchy
  "Returns a graph of the project's dependencies."
  [dependencies-key project]
  (aether/dependency-hierarchy
   (project dependencies-key)
   (get-dependencies dependencies-key project)))

(defn- normalize-path [root path]
  (let [f (io/file path)]
    (.getAbsolutePath (if (.isAbsolute f) f (io/file root path)))))

(defn get-classpath
  "Return a the classpath for project as a list of strings."
  [project]
  (for [path (concat (:test-paths project)
                     (:source-paths project)
                     (:resource-paths project)
                     [(:compile-path project)]
                     (checkout-deps-paths project)
                     (for [dep (resolve-dependencies :dependencies project)]
                       (.getAbsolutePath dep)))
        :when path]
    (normalize-path (:root project) path)))
