(ns leiningen.test.test
  (:refer-clojure :exclude [test])
  (:require [clojure.test :refer :all]
            [leiningen.test :refer :all]
            [leiningen.test.helper :refer [tmp-dir sample-no-aot-project
                                           sample-failing-project abort-msg]]
            [clojure.java.io :as io]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]))

(use-fixtures :each
              (fn [f]
                (f)
                (.delete (java.io.File. tmp-dir "lein-test-ran"))))

(defn runs []
  (let [ran-file (io/file tmp-dir "lein-test-ran")]
    (and (.exists ran-file)
         (-> ran-file
             (slurp)
             (.split "\n")
             (->> (map read-string)
                  (frequencies))))))

(defn ran? [] (-> (runs) keys set))

(deftest test-project-selectors
  (is (= [:default :integration :int2 :no-custom]
           (keys (:test-selectors sample-no-aot-project))))
  (is (every? ifn? (map eval (vals (:test-selectors sample-no-aot-project))))))

(deftest test-default-selector
  (test sample-no-aot-project ":default")
  (is (= (ran?) #{:regular :int2 :not-custom :fixture})))

(deftest fixture-runs-appropriate-number-of-times
  ;; Issue #1269
  (test sample-no-aot-project)
  ;; Because three tests ran
  (is (= 3 ((runs) :fixture))))

(deftest test-no-args-defaults-to-default-selector
  (test sample-no-aot-project)
  (is (= (ran?) #{:regular :int2 :not-custom :fixture})))

(deftest test-basic-selector
  (test sample-no-aot-project ":integration")
  (is (= (ran?) #{:integration :integration-ns :fixture})))

(deftest test-complex-selector
  (test sample-no-aot-project ":no-custom")
  (is (= (ran?) #{:integration :integration-ns :regular :int2 :fixture})))

(deftest test-two-selectors
  (test sample-no-aot-project ":integration" ":int2")
  (is (= (ran?) #{:integration :integration-ns :int2 :fixture})))

(deftest test-override-namespace-selector
  (test sample-no-aot-project ":int2")
  (is (= (ran?) #{:integration-ns :int2 :fixture})))

(deftest test-only-selector
  (test sample-no-aot-project ":only" "selectors/regular")
  (is (= (ran?) #{:regular :fixture})))

(deftest test-namespace-argument
  (test sample-no-aot-project "selectors")
  (is (= (ran?) #{:regular :not-custom :int2 :fixture})))

(deftest test-invalid-namespace-argument
  (is (.contains
       (abort-msg
        test sample-no-aot-project "boom")
       "java.io.FileNotFoundException: Could not locate")))

(deftest test-file-argument
  (let [file (io/file (first (:test-paths sample-no-aot-project)) "selectors.clj")]
    (test sample-no-aot-project (.getPath file)))
  (is (= (ran?) #{:regular :not-custom :int2 :fixture})))

(deftest test-unreadable-test-fails
  (let [project (project/merge-profiles sample-failing-project
                                        [{:aot ^:replace []
                                          :dependencies ^:replace
                                          [['org.clojure/clojure "1.5.1"]]}])]
    (binding [main/*exit-process?* false]
      (is (= (read-args [""] project) ['sample.unreadable]))
      (is (try (test project)
               false
               (catch Exception e
                 (= "Tests failed." (.getMessage e))))))))
