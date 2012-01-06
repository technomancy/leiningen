(ns leiningen.test.new
  (:require [leiningen.new])
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.util.file :only [delete-file-recursively]]))

(deftest test-new
  (leiningen.new/new nil "test-new-proj")
  (is (= ["README.md" "project.clj" "src" "test_new_proj" "core.clj" "test"
          "test_new_proj" "core_test.clj" ".gitignore"]
         (map (memfn getName) (rest (file-seq (file "test-new-proj"))))))
  (delete-file-recursively (file "test-new-proj") false))
