(ns leiningen.test.pom
  (:use [clojure.test]
        [clojure.java.io :only [file delete-file]]
        [leiningen.core :only [read-project defproject]]
        [leiningen.util.maven :only [make-model]]
        [leiningen.pom :only [pom]]
        [leiningen.test.helper :only [sample-project]]))

(deftest test-pom
  (let [pom-file (file (:root sample-project) "pom.xml")]
    (delete-file pom-file true)
    (pom sample-project)
    (is (.exists pom-file))))

(deftest test-make-model-includes-build-settings
  (let [model (make-model sample-project)]
    (is (= "src" (-> model .getBuild .getSourceDirectory)))
    (is (= "test" (-> model .getBuild .getTestSourceDirectory)))))

(deftest test-snapshot-checking
  (let [aborted? (atom false)]
    (binding [leiningen.core/abort #(reset! aborted? %&)]
      (pom (assoc sample-project :version "1.0"
                  :dependencies [['clojure "1.0.0-SNAPSHOT"]]))
      (is @aborted?))))
