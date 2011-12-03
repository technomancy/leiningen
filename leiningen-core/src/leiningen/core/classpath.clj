(ns leiningen.core.classpath
  "Calculate project classpaths by resolving dependencies via Aether."
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.java.io :as io]
            [leiningen.core.user :as user]
            [leiningen.core.project :as project]))

;; Basically just for re-throwing a more comprehensible error.
(defn- read-dependency-project [dep]
  (let [project (.getAbsolutePath (io/file dep "project.clj"))]
    (try (project/read project)
         (catch Exception e
           (throw (Exception. (format "Problem loading %s" project) e))))))

(defn- checkout-deps-paths
  "Checkout dependencies are used to place source for a dependency
  project directly on the classpath rather than having to install the
  dependency and restart the dependent project."
  [project]
  (apply concat (for [dep (.listFiles (io/file (:root project) "checkouts"))
                      :let [proj (read-dependency-project dep)]]
                  ;; TODO: honor profile transitively?
                  (for [d (:checkout-deps-shares project [:source-path
                                                          :compile-path
                                                          :resources-path])]
                    (str (io/file (:root project) "checkouts" (.getName dep) (d proj)))))))

;; Ideally pomegranate would accept map forms for repositories so you
;; could do things like toggling snapshots and such, but for now we
;; normalize back to url-as-string.

;; TODO: add authentication to repositories
;; TODO: add policies to repositories

(defn- repositories-map [repositories]
  (into {} (for [[id repo] repositories]
             [id (:url repo)])))

(defn resolve-dependencies
  "Simply delegate regular dependencies to pomegranate. This will
  ensure they are downloaded into ~/.m2/repositories."
  [{:keys [repositories dependencies]}]
  {:pre [(every? vector? dependencies)]}
  (aether/resolve-dependencies :repositories (repositories-map repositories)
                               :coordinates dependencies))

(defn resolve-dev-dependencies
  "Dev dependencies need to be copied into lib/dev since they need to
  be on Leiningen's classpath as well as the project's classpath, and
  Leiningen's cannot be calculated from Clojure; it must be known when
  the shell script is started."
  [{:keys [repositories dev-dependencies root]}]
  {:pre [(every? vector? dev-dependencies)]}
  (let [files (aether/resolve-dependencies :repositories (repositories-map repositories)
                                           :coordinates dev-dependencies)]
    (when (seq dev-dependencies)
      (.mkdirs (io/file root "lib/dev")))
    (doseq [file files]
      ;; TODO: does lib/dev still make sense as a dev-deps location?
      (io/copy file (io/file root "lib/dev" (.getName file))))
    files))

(defn- normalize-path [root path]
  (when path
    (let [f (io/file path)]
      (.getAbsolutePath (if (.isAbsolute f) f (io/file root path))))))

(defn get-classpath
  "Return a the classpath for project as a list of strings."
  [project]
  ;; TODO: figure out if these should be excluded via profiles or what
  (for [path (concat [(:test-path project)
                      (:dev-resources-path project)]
                     [(:source-path project)
                      (:compile-path project)
                      (:resources-path project)]
                     (:extra-classpath-dirs project)
                     (checkout-deps-paths project)
                     (map #(.getAbsolutePath %)
                          (resolve-dev-dependencies project))
                     (map #(.getAbsolutePath %)
                          (resolve-dependencies project))
                     ;; TODO: exclude outside dev contexts
                     (user/plugins))
        :when path]
    (normalize-path (:root project) path)))
