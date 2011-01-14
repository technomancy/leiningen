(ns test-test
  (:refer-clojure :exclude [test])
  (:use [leiningen.test]
        [leiningen.util.file :only [tmp-dir]]
        [leiningen.core :only [read-project]] :reload)
  (:use [clojure.test]))

(use-fixtures :each
              (fn [f]
                (f)
                (.delete (java.io.File. tmp-dir "lein-test-ran"))))

(def project (binding [*ns* (find-ns 'leiningen.core)]
               (read-project "test_projects/sample_no_aot/project.clj")))

(defn ran? [& expected]
  (= (set expected)
     (set (map read-string (.split (slurp (format "%s/lein-test-ran" tmp-dir))
                                   "\n")))))

(deftest test-project-selectors
  (is (= [:default :integration :int2 :no-custom]
           (keys (:test-selectors project))))
  (is (every? ifn? (map eval (vals (:test-selectors project))))))

(deftest test-default-selector
  (test project ":default")
  (is (ran? :regular :int2 :not-custom)))

(deftest test-basic-selector
  (test project ":integration")
  (is (ran? :integration)))

(deftest test-complex-selector
  (test project ":no-custom")
  (is (ran? :integration :regular :int2)))

(deftest test-two-selectors
  (test project ":integration" ":int2")
  (is (ran? :integration :int2)))
