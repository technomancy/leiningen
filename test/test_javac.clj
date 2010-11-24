(ns test-javac
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.javac :only [javac]]
        [leiningen.core :only [read-project]]))

(def test-project (read-project "test_projects/dev_deps_only/project.clj"))

(deftest test-javac
  (javac test-project)
  (is (.exists (file "test_projects/dev-deps-only/classes"
                     "dev_deps_only" "Junk.class"))))
