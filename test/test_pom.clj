(ns test-pom
  (:use [leiningen.core :only [read-project defproject]]
        [leiningen.util.maven :only [make-model]]
        [leiningen.pom :only [pom]])
  (:use [clojure.test]
        [clojure.java.io :only [file delete-file]]))

(def test-project (read-project "test_projects/sample/project.clj"))

(deftest test-pom
  (let [pom-file (file (:root test-project) "pom.xml")]
    (delete-file pom-file true)
    (pom test-project)
    (is (.exists pom-file))))

(deftest test-make-model-includes-build-settings
  (let [model (make-model test-project)]
    (is (= "src" (-> model .getBuild .getSourceDirectory)))
    (is (= "test" (-> model .getBuild .getTestSourceDirectory)))))

(deftest test-snapshot-checking
  (is (thrown? Exception (pom (assoc test-project
                                :version "1.0"
                                :dependencies [['clojure "1.0.0-SNAPSHOT"]])))))
