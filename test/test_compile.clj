(ns test-compile
  (:use [clojure.test]
        [clojure.contrib.java-utils :only [delete-file-recursively file]]
        [clojure.contrib.shell-out :only [with-sh-dir sh]]))

(deftest test-compile
  (delete-file-recursively (file "sample" "classes" "nom") true)
  (with-sh-dir "sample"
    (sh "lein" "compile"))
  (is (.exists (file "sample" "classes" "nom" "nom" "nom.class"))))
