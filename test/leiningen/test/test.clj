(ns leiningen.test.test
  (:refer-clojure :exclude [test])
  (:use [clojure.test]
        [leiningen.test]
        [leiningen.util.file :only [tmp-dir]]
        [leiningen.core :only [read-project]]
        [leiningen.test.helper :only [sample-no-aot-project]]))

(use-fixtures :each
              (fn [f]
                (f)
                (.delete (java.io.File. tmp-dir "lein-test-ran"))))

(defn ran? [& expected]
  (= (set expected)
     (set (map read-string (.split (slurp (format "%s/lein-test-ran" tmp-dir))
                                   "\n")))))

(deftest test-project-selectors
  (is (= [:default :integration :int2 :no-custom]
           (keys (:test-selectors sample-no-aot-project))))
  (is (every? ifn? (map eval (vals (:test-selectors sample-no-aot-project))))))

(deftest test-default-selector
  (test sample-no-aot-project ":default")
  (is (ran? :regular :int2 :not-custom)))

(deftest test-basic-selector
  (test sample-no-aot-project ":integration")
  (is (ran? :integration)))

(deftest test-complex-selector
  (test sample-no-aot-project ":no-custom")
  (is (ran? :integration :regular :int2)))

(deftest test-two-selectors
  (test sample-no-aot-project ":integration" ":int2")
  (is (ran? :integration :int2)))
