(ns leiningen.clean
  "Remove all files from project's target-path."
  (:require [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.utils :as utils])
  (:import [java.io IOException]))

(defn real-directory?
  "Returns true if this file is a real directory, false if it is a symlink or a
  normal file."
  [f]
  (if (= :windows (eval/get-os))
    (.isDirectory f)
    (and (.isDirectory f)
         (not (utils/symlink? f)))))

(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (io/file f)]
    (when (real-directory? f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (.setWritable f true)
    (io/delete-file f silently)))

(defn- ancestor?
  "Is a an ancestor of b?"
  [a b]
  (let [hypothetical-ancestor (.getCanonicalPath (io/file a))
        hypothetical-descendant (.getCanonicalPath (io/file b))]
    (and (.startsWith hypothetical-descendant hypothetical-ancestor)
         (not (= hypothetical-descendant hypothetical-ancestor)))))

(defn- protected-paths
  "Returns a set of leiningen project source directories and important files."
  [project]
  (->> [:source-paths :java-source-paths :test-paths :resource-paths]
     (select-keys project)
     vals
     flatten
     (cons "doc")
     (cons "project.clj")
     (map io/file)
     (map #(.getCanonicalPath %))
     set))

(defn- protected-path?
  "Is dir one of the leiningen project files or directories (which we expect to be version controlled), or a descendant?"
  [project dir]
  (let [protected-paths (protected-paths project)]
    (or (protected-paths (.getCanonicalPath (io/file dir)))
        (some #(ancestor? % dir) protected-paths))))

(defn- sanity-check
  "Ensure that a clean-target string refers to a directory that is sensible to delete."
  [project clean-target]
  (when (string? clean-target)
    (cond (not (ancestor? (:root project) clean-target))
          (throw (IOException. "Deleting a directory outside of the project root is not allowed."))
          (protected-path? project clean-target)
          (throw (IOException. "Deleting non-target project directories is not allowed.")))))

(defn clean
  "Remove all files from paths in project's clean-targets."
  [project]
  (doseq [target-key (:clean-targets project)]
    (when-let [target (cond (vector? target-key) (get-in project target-key)
                            (keyword? target-key) (target-key project)
                            (string? target-key) target-key)]
      (doseq [f (flatten [target])]
        (sanity-check project f)
        (delete-file-recursively f :silently)))))
