(ns test-deps
  (:use [leiningen.core :only [read-project defproject]]
        [leiningen.deps :only [deps]] :reload-all)
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.util.file :only [delete-file-recursively]]))

(def test-project (read-project "test_projects/sample/project.clj"))

(deftest test-deps
  (delete-file-recursively (file (:root test-project) "lib") true)
  (deps test-project)
  (let [jars (set (map #(.getName %)
                       (.listFiles (file (:root test-project) "lib"))))]
    (doseq [j ["jdom-1.0.jar" "tagsoup-1.2.jar" "rome-0.9.jar"]]
      (is (jars j)))))

(def dev-deps-only-project
  (read-project "test_projects/dev-deps-only/project.clj"))

(deftest test-dev-deps-only
  (delete-file-recursively (file (:root dev-deps-only-project) "lib") true)
  (deps dev-deps-only-project)
  (let [jars (set (map #(.getName %)
                       (.listFiles (file (:root dev-deps-only-project) "lib" "dev"))))]
    (doseq [j ["clojure-1.2.0.jar"]]
      (is (jars j)))))

