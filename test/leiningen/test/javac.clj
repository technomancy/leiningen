(ns leiningen.test.javac
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.javac :only [javac]]
        [leiningen.core :only [read-project defproject]]
        [leiningen.util.file :only [delete-file-recursively]]))

(def test-project (read-project "test_projects/dev-deps-only/project.clj"))

(deftest test-javac
  (delete-file-recursively "test_projects/dev-deps-only/classes" true)
  (javac test-project)
  (is (.exists (file "test_projects/dev-deps-only/classes"
                     "dev_deps_only" "Junk.class")))
  (is (.exists (file "test_projects/dev-deps-only/classes"
                     "dev_deps_only" "Junk2.class"))))
