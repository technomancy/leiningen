(ns test-pom
  (:use [leiningen.core :only [read-project defproject]]
        [leiningen.pom :only [pom make-model]])
  (:use [clojure.test]
        [clojure.contrib.io :only [file delete-file]]))

(def test-project (read-project "sample/project.clj"))

(deftest test-pom
  (let [pom-file (file (:root test-project) "pom.xml")]
    (delete-file pom-file true)
    (pom test-project)
    (is (.exists pom-file))))

(deftest test-make-model-includes-build-settings
  (let [model (make-model test-project)]
    (is (= "src" (-> model .getBuild .getSourceDirectory)))
    (is (= "test" (-> model .getBuild .getTestSourceDirectory)))))
