(ns leiningen.test.new
  (:require [leiningen.new])
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.test.helper :only [delete-file-recursively]]))

(deftest test-new-with-just-project-name
  (leiningen.new/new nil "test-new-proj")
  (is (= #{"README.md" "project.clj" "src" "core.clj" "test"
           "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"}
         (set (map (memfn getName) (rest (file-seq (file "test-new-proj")))))))
  (delete-file-recursively (file "test-new-proj") :silently))

(deftest test-new-with-group-and-project-name
  (leiningen.new/new nil "orgname/a-project")
  (is (= #{"README.md" "project.clj" "orgname" "src" "core.clj" "test"
           "doc" "intro.md" "a_project" "core_test.clj" ".gitignore"}
         (set (map (memfn getName)
                   (rest (file-seq (file "a-project")))))))
  (delete-file-recursively (file "a-project") :silently))
