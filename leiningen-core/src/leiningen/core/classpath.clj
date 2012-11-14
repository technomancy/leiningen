(ns leiningen.core.classpath
  "Calculate project classpaths by resolving dependencies via Aether."
  (:require [cemerick.pomegranate.aether :as aether]
            [cemerick.pomegranate :as pomegranate]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.user :as user])
  (:import (java.util.jar JarFile)
           (java.net URL)
           (org.sonatype.aether.resolution DependencyResolutionException)))

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

(defn- checkout-dep-paths [project dep-project]
  ;; can't mapcat here since :checkout-deps-shares points to vectors and strings
  (flatten (map dep-project (:checkout-deps-shares project))))

(defn- checkout-deps-paths
  "Checkout dependencies are used to place source for a dependency
  project directly on the classpath rather than having to install the
  dependency and restart the dependent project."
  [project]
  (apply concat (for [dep (.list (io/file (:root project) "checkouts"))
                      :let [dep-project (read-dependency-project
                                         (:root project) dep)]
                      :when dep-project]
                  (checkout-dep-paths project dep-project))))

(defn extract-native-deps [deps native-path]
  (doseq [jar (map #(JarFile. %) deps)
          entry (enumeration-seq (.entries jar))
          :when (.startsWith (.getName entry) "native/")]
    (let [f (io/file native-path (subs (.getName entry) (count "native/")))]
      (if (.isDirectory entry)
        (.mkdirs f)
        (do (.mkdirs (.getParentFile f))
            (io/copy (.getInputStream jar entry) f))))))

(defn when-stale
  "Call f with args when keys in project.clj have changed since the last
  run. Stores value of project keys in stale directory inside :target-path.
  Because multiple callers may check the same keys, you must also provide a
  token to keep your stale value separate. Returns true if the code was executed
  and nil otherwise."
  [token keys project f & args]
  (let [file (io/file (:target-path project) "stale"
                      (str (name token) "." (str/join "+" (map name keys))))
        current-value (pr-str (map (juxt identity project) keys))
        old-value (and (.exists file) (slurp file))]
    (when (and (:name project) (:target-path project)
               (not= current-value old-value))
      (apply f args)
      (.mkdirs (.getParentFile file))
      (spit file (doall current-value))
      true)))

(defn add-repo-auth
  "Repository credentials (a map containing some of
  #{:username :password :passphrase :private-key-file}) are discovered
  from:

  1. Looking up the repository URL in the ~/.lein/credentials.clj.gpg map
  2. Scanning that map for regular expression keys that match the
     repository URL.

  So, a credentials map that contains an entry:

    {#\"http://maven.company.com/.*\" {:username \"abc\" :password \"xyz\"}}

  would be applied to all repositories with URLs matching the regex key
  that didn't have an explicit entry."
  [[id repo]]
  [id (-> repo user/profile-auth user/resolve-credentials)])

(defn get-proxy-settings
  "Returns a map of the JVM proxy settings"
  ([] (get-proxy-settings "http_proxy"))
  ([key]
     (if-let [proxy (System/getenv key)]
       (let [url (try (URL. proxy)
                      (catch java.net.MalformedURLException _
                        (URL. (str "http://" proxy))))
             user-info (.getUserInfo url)
             [username password] (and user-info (.split user-info ":"))]
         {:host (.getHost url)
          :port (.getPort url)
          :username username
          :password password
          :non-proxy-hosts (System/getenv "http_no_proxy")}))))

(defn- update-policies [update checksum [repo-name opts]]
  [repo-name (merge {:update (or update :daily)
                     :checksum (or checksum :fail)} opts)])

(defn- root-cause [e]
  (last (take-while identity (iterate (memfn getCause) e))))

(defn- get-dependencies
  [dependencies-key {:keys [repositories local-repo offline? update
                            checksum mirrors] :as project}
   & {:keys [add-classpath?]}]
  {:pre [(every? vector? (get project dependencies-key))]}
  (try
    ((if add-classpath?
       pomegranate/add-dependencies
       aether/resolve-dependencies)
     :local-repo local-repo
     :offline? offline?
     :repositories (->> repositories
                        (map add-repo-auth)
                        (map (partial update-policies update checksum)))
     :coordinates (get project dependencies-key)
     :mirrors mirrors
     :transfer-listener
     (bound-fn [e]
       (let [{:keys [type resource error]} e]
         (let [{:keys [repository name size]} resource]
           (case type
             :started
             (println "Retrieving"
                      name
                      (if (neg? size)
                        ""
                        (format "(%sk)"
                                (Math/round (double (max 1 (/ size 1024))))))
                      "from"
                      (or (first (first (filter #(= repository (:url (second %)))
                                                repositories)))
                          repository))
             :failed
             (if (and (= repository (:url (second (last repositories))))
                      error)
               (println "Failed to find" name))
             nil))))
     :proxy (get-proxy-settings))
    (catch DependencyResolutionException e
      (binding [*out* *err*]
        (println "This could be due to a typo in :dependencies or network issues.")
        (when-not (some #(= "https://clojars.org/repo/" (:url (second %))) repositories)
          (println "It's possible the specified jar is in the old Clojars Classic repo.")
          (println "If so see https://github.com/ato/clojars-web/wiki/Releases.")))
      (throw (ex-info "Could not resolve dependencies" {:exit-code 1})))
    (catch Exception e
      (if (and (instance? java.net.UnknownHostException (root-cause e))
               (not offline?))
        (get-dependencies dependencies-key (assoc project :offline? true))
        (throw e)))))

(defn resolve-dependencies
  "Delegate dependencies to pomegranate. This will ensure they are
  downloaded into ~/.m2/repository and that native components of
  dependencies have been extracted to :native-path. If :add-classpath?
  is logically true, will add the resolved dependencies to Leiningen's
  classpath.

  Returns a seq of the dependencies' files."
  [dependencies-key {:keys [repositories native-path] :as project} & rest]
  (let [jars (->> (apply get-dependencies dependencies-key project rest)
                  (aether/dependency-files)
                  (filter #(re-find #"\.(jar|zip)$" (.getName %))))]
    (when-not (= :plugins dependencies-key)
      (or (when-stale :extract-native [dependencies-key] project
                      extract-native-deps jars native-path)
          ;; Always extract native deps from SNAPSHOT jars.
          (extract-native-deps (filter #(re-find #"SNAPSHOT" (.getName %)) jars)
                               native-path)))
    jars))

(defn dependency-hierarchy
  "Returns a graph of the project's dependencies."
  [dependencies-key project]
  (aether/dependency-hierarchy
   (get project dependencies-key)
   (get-dependencies dependencies-key project)))

(defn- normalize-path [root path]
  (let [f (io/file path)]
    (.getAbsolutePath (if (.isAbsolute f) f (io/file root path)))))

(defn ext-dependency?
  "Should the given dependency be loaded in the extensions classloader?"
  [dep]
  (second
   (some #(if (= :ext (first %)) dep)
         (partition 2 dep))))

(defn ext-classpath
  "Classpath of the extensions dependencies in project as a list of strings."
  [project]
  (seq
   (->> (filter ext-dependency? (:dependencies project))
        (assoc project :dependencies)
        (resolve-dependencies :dependencies)
        (map (memfn getAbsolutePath)))))

(defn get-classpath
  "Return the classpath for project as a list of strings."
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
