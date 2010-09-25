(ns test-test
  (:refer-clojure :exclude [test])
  (:use [leiningen.test]
        [leiningen.core :only [read-project]] :reload)
  (:use [clojure.test]))

(use-fixtures :each
              (fn [f]
                (.delete (java.io.File. "/tmp/lein-test-ran"))
                (f)))

(def project (binding [*ns* (find-ns 'leiningen.core)]
               (read-project "test_projects/sample_no_aot/project.clj")))

(defn ran []
  (map read-string (.split (slurp "/tmp/lein-test-ran") "\n")))

(deftest test-no-selectors
  (test project)
  (is (= [:regular :integration :int2 :not-custom] (ran))))

(deftest test-basic-selector
  (test project ":integration")
  (is (= [:integration] (ran))))

(deftest test-two-selectors
  (test project ":integration" ":int2")
  (is (= [:integration :int2] (ran))))
