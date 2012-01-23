(ns leiningen.test.pom
  (:use [clojure.test]
        [clojure.java.io :only [file delete-file]]
        [leiningen.pom :only [pom]]
        [leiningen.test.helper :only [sample-project]]))

(deftest test-pom
  (let [pom-file (file (:root sample-project) "pom.xml")]
    (delete-file pom-file true)
    (pom sample-project)
    (is (.exists pom-file))))

(deftest test-snapshot-checking
  (let [aborted? (atom false)]
    (binding [leiningen.pom/abort #(reset! aborted? %&)]
      (let [project (assoc sample-project :version "1.0"
                           :dependencies [['clojure "1.0.0-SNAPSHOT"]])]
        (pom (with-meta project
             {:without-profiles project})))
      (is @aborted?))))