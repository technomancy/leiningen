(ns leiningen.test.helper
  (:use [clojure.java.io :only [file]]
        [leiningen.core :only [read-project]]))

(def local-repo (file (System/getProperty "user.home") ".m2" "repository"))

(defn m2-dir [n v]
  (file local-repo (name n) (if (string? n) n (namespace n)) v))

(defn- read-test-project [name]
  (binding [*ns* (find-ns 'leiningen.core)]
    (read-project (format "test_projects/%s/project.clj" name))))

(def sample-project (read-test-project "sample"))

(def dev-deps-project (read-test-project "dev-deps-only"))

(def sample-failing-project (read-test-project "sample_failing"))

(def sample-no-aot-project (read-test-project "sample_no_aot"))

(def tricky-name-project (read-test-project "tricky-name"))
