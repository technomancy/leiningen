(ns leiningen.test.pom
  (:use [clojure.test]
        [clojure.java.io :only [file delete-file]]
        [leiningen.pom :only [make-pom pom]]
        [leiningen.test.helper :only [sample-project]]))

(deftest test-pom-file-is-created
  (let [pom-file (file (:target-path sample-project) "pom.xml")]
    (delete-file pom-file true)
    (pom sample-project)
    (is (.exists pom-file))))

(deftest test-pom-has-classifier-when-defined
  (let [pom (make-pom sample-project)]
    (is (not (re-find #"classifier" pom))))
  (let [altered-meta (assoc-in (meta sample-project)
                               [:without-profiles :classifier]
                               "stuff")
        pom (make-pom (with-meta sample-project altered-meta))]
    (is (re-find #"<classifier>stuff</classifier>" pom))))


(deftest test-snapshot-checking
  (let [aborted? (atom false)]
    (binding [leiningen.pom/abort #(reset! aborted? %&)]
      (let [project (assoc sample-project :version "1.0"
                           :dependencies [['clojure "1.0.0-SNAPSHOT"]])]
        (pom (with-meta project
             {:without-profiles project})))
      (is @aborted?))))