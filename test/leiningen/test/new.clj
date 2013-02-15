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
  (is (= #{"src" "a_project_test.clj" "project.clj" "a_project.clj" "orgname"
           "test" ".gitignore" "README.md" "doc" "intro.md"}
         (set (map (memfn getName)
                   (rest (file-seq (file "a-project")))))))
  (delete-file-recursively (file "a-project") :silently))

(deftest test-new-with-to-dir-option
  (leiningen.new/new nil "test-new-proj" "--to-dir" "my-proj")
  (is (= #{"README.md" "project.clj" "src" "core.clj" "test"
           "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"}
         (set (map (memfn getName) (rest (file-seq (file "my-proj")))))))
  (delete-file-recursively (file "my-proj") :silently))