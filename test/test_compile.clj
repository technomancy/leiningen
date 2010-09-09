(ns test-compile
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [clojure.java.shell :only [with-sh-dir sh]]
        [leiningen.util.file :only [delete-file-recursively]]))

(deftest test-compile
  (delete-file-recursively (file "test_projects" "sample" "classes" "nom") true)
  (with-sh-dir (file "test_projects" "sample")
    (is (zero? (:exit (sh "lein" "compile" :return-map true)))))
  (is (.exists (file "test_projects" "sample"
                     "classes" "nom" "nom" "nom.class")))
  (with-sh-dir (file "test_projects" "sample_failing")
    (is (not (zero? (:exit (sh "lein" "compile" :return-map true)))))))
