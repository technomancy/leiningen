(ns leiningen.test.deps
  (:use [leiningen.core :only [read-project defproject]]
        [leiningen.deps :only [deps]]
        [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.util.file :only [delete-file-recursively]]
        [leiningen.test.helper :only [sample-project dev-deps-project]]))

(deftest test-deps
  (delete-file-recursively (file (:root sample-project) "lib") true)
  (deps sample-project)
  (let [jars (set (map #(.getName %)
                       (.listFiles (file (:root sample-project) "lib"))))]
    (doseq [j ["jdom-1.0.jar" "tagsoup-1.2.jar" "rome-0.9.jar"]]
      (is (jars j)))))

(deftest test-dev-deps-only
  (delete-file-recursively (file (:root dev-deps-project) "lib") true)
  (deps dev-deps-project)
  (let [jars (set (map #(.getName %)
                       (.listFiles (file (:root dev-deps-project)
                                         "lib" "dev"))))]
    (doseq [j ["clojure-1.2.0.jar"]]
      (is (jars j)))))
