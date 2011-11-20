(ns leiningen.core.classpath
  "Calculate classpath by resolving dependencies."
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.java.io :as io]
            [leiningen.core.user :as user]))

(defn- checkout-deps-paths [project]
  ;; TODO: needs read-project
  #_(apply concat (for [dep (.listFiles (file (:root project) "checkouts"))
                      ;; Note that this resets the leiningen.core/project var!
                      :let [proj (read-dependency-project dep)]
                      :when proj]
                  (for [d (:checkout-deps-shares project [:source-path
                                                          :compile-path
                                                          :resources-path])]
                    (ensure-absolute (d proj) dep)))))

(defn resolve-dependencies [{:keys [repositories dependencies]}]
  {:pre [(every? vector? dependencies)]}
  (aether/resolve-dependencies :repositories (into {} repositories)
                               :coordinates dependencies))

(defn resolve-dev-dependencies [{:keys [repositories dev-dependencies root]}]
  {:pre [(every? vector? dev-dependencies)]}
  (let [files (aether/resolve-dependencies :repositories (into {} repositories)
                                           :coordinates dev-dependencies)]
    (when (seq dev-dependencies)
      (.mkdirs (io/file root "lib/dev")))
    (doseq [file files]
      ;; TODO: does lib/dev still make sense as a dev-deps location?
      (io/copy file (io/file root "lib/dev" (.getName file))))
    files))

(defn get-classpath
  "Return a the classpath for project as a list of strings."
  [project]
  ;; TODO: figure out if these should be excluded via profiles or what
  (remove nil? (concat [(:test-path project)
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
                       (user/plugins))))
