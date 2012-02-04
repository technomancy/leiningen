(ns leiningen.core.classpath
  "Calculate project classpaths by resolving dependencies via Aether."
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.java.io :as io]
            [leiningen.core.project :as project])
  (:import java.util.jar.JarFile))

;; Basically just for re-throwing a more comprehensible error.
(defn- read-dependency-project [root dep]
  (let [project (.getAbsolutePath (io/file root "checkouts" dep "project.clj"))]
    (try (project/read project [])
         (catch Exception e
           (throw (Exception. (format "Problem loading %s" project) e))))))

(defn- checkout-dep-paths [project dep dep-project]
  (concat (:source-path dep-project)
          (:resources-path dep-project)
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

;; TODO: add authentication to repositories
;; TODO: add policies to repositories
;; TODO: ensure repositories is ordered

(defn extract-native-deps [deps project]
  (doseq [jar (map #(JarFile. %) deps)
          entry (enumeration-seq (.entries jar))
          :when (.startsWith (.getName entry) "native/")]
    (let [f (io/file (:native-path project) (subs (.getName entry)
                                                  (count "native/")))]
      (if (.isDirectory entry)
        (.mkdirs f)
        (io/copy (.getInputStream jar entry) f)))))

(defn resolve-dependencies
  "Simply delegate regular dependencies to pomegranate. This will
  ensure they are downloaded into ~/.m2/repositories and that native
  deps have been extracted to :native-path."
  [{:keys [repositories dependencies] :as project}]
  {:pre [(every? vector? dependencies)]}
  (doto (set (aether/dependency-files
               (aether/resolve-dependencies :repositories repositories
                                            :coordinates dependencies)))
    (extract-native-deps project)))

(defn- normalize-path [root path]
  (let [f (io/file path)]
    (.getAbsolutePath (if (.isAbsolute f) f (io/file root path)))))

(defn get-classpath
  "Return a the classpath for project as a list of strings."
  [project]
  (for [path (concat (:test-path project)
                     (:source-path project)
                     (:resources-path project)
                     [(:compile-path project)]
                     (checkout-deps-paths project)
                     (map #(.getAbsolutePath %) (resolve-dependencies project)))
        :when path]
    (normalize-path (:root project) path)))
