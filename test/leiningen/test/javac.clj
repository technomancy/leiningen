(ns leiningen.test.javac
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.javac :only [javac]]
        [leiningen.test.helper :only [delete-file-recursively
                                      #_dev-deps-project]]))

(deftest ^:post-preview ; not really; need to fix this
  test-javac
  #_(delete-file-recursively (:compile-path dev-deps-project) true)
  #_(javac dev-deps-project)
  (is (.exists (file "test_projects/dev-deps-only/classes"
                     "dev_deps_only" "Junk.class")))
  (is (.exists (file "test_projects/dev-deps-only/classes"
                     "dev_deps_only" "Junk2.class"))))
