(ns leiningen.core.classpath
  "Calculate project classpaths by resolving dependencies via Aether."
  (:require [cemerick.pomegranate.aether :as aether]
            [cemerick.pomegranate :as pomegranate]
            [clojure.java.io :as io]
            [leiningen.core.user :as user])
  (:import java.util.jar.JarFile))

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
  (concat (:source-paths dep-project)
          (:resource-paths dep-project)
          [(:compile-path dep-project)]))

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
    (let [f (io/file native-path (subs (.getName entry)
                                       (count "native/")))]
      (if (.isDirectory entry)
        (.mkdirs f)
        (io/copy (.getInputStream jar entry) f)))))

(defn add-repo-auth [[id {:keys [url] :as repo}]]
  "Repository credentials (a map containing some of
   #{:username :password :passphrase :private-key-file}) are discovered
   from:

   1. Looking up the repository URL in the [:auth :repository-auth]
      map
   2. Scanning that map for regular expression keys that match the
      repository URL.

   So, a :repository-auth map that contains an entry:

     {#\"http://maven.company.com/.*\" {:username \"abc\" :password \"xyz\"}}

   would be applied to all repositories with URLs matching the regex key
   that didn't have an explicit entry."
  (let [repo-creds (-> (user/profiles) :auth :repository-auth)]
    (if-let [match (get repo-creds url)]
      [id (merge repo match)]
      [id (merge repo (first (for [[re? cred] repo-creds 
                                   :when (and (instance? java.util.regex.Pattern re?)
                                              (re-matches re? url))]
                               cred)))])))

(defn add-auth [repositories]
  (map add-repo-auth repositories))

(defn resolve-dependencies
  "Simply delegate regular dependencies to pomegranate. This will
  ensure they are downloaded into ~/.m2/repositories and that native
  deps have been extracted to :native-path.  If :add-classpath? is
  logically true, will add the resolved dependencies to Leiningen's
  classpath.

   Returns a set of the dependencies' files."
  [dependencies-key {:keys [repositories native-path] :as project} & {:keys [add-classpath?]}]
  {:pre [(every? vector? (project dependencies-key))]}
  (doto (set (aether/dependency-files
              ((if add-classpath?
                 pomegranate/add-dependencies
                 aether/resolve-dependencies)
                :repositories (add-auth repositories)
                :coordinates (project dependencies-key)
                :transfer-listener :stdout)))
    (extract-native-deps native-path)))

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
                     (map #(.getAbsolutePath %) (resolve-dependencies :dependencies project)))
        :when path]
    (normalize-path (:root project) path)))
