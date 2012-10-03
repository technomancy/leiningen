(ns leiningen.test.test
  (:refer-clojure :exclude [test])
  (:use [clojure.test]
        [leiningen.test]
        [leiningen.test.helper :only [tmp-dir sample-no-aot-project]])
  (:require [clojure.java.io :as io]))

(use-fixtures :each
              (fn [f]
                (f)
                (.delete (java.io.File. tmp-dir "lein-test-ran"))))

(defn ran? []
  (let [ran-file (io/file tmp-dir "lein-test-ran")]
    (and (.exists ran-file)
         (set (for [ran (.split (slurp ran-file) "\n")]
                (read-string ran))))))

(deftest test-project-selectors
  (is (= [:default :integration :int2 :no-custom]
           (keys (:test-selectors sample-no-aot-project))))
  (is (every? ifn? (map eval (vals (:test-selectors sample-no-aot-project))))))

(deftest test-default-selector
  (test sample-no-aot-project ":default")
  (is (= (ran?) #{:regular :int2 :not-custom})))

(deftest test-basic-selector
  (test sample-no-aot-project ":integration")
  (is (= (ran?) #{:integration :integration-ns})))

(deftest test-complex-selector
  (test sample-no-aot-project ":no-custom")
  (is (= (ran?) #{:integration :integration-ns :regular :int2})))

(deftest test-two-selectors
  (test sample-no-aot-project ":integration" ":int2")
  (is (= (ran?) #{:integration :integration-ns :int2})))

(deftest test-override-namespace-selector
  (test sample-no-aot-project ":int2")
  (is (= (ran?) #{:integration-ns :int2})))
