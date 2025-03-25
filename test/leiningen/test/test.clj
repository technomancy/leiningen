(ns leiningen.test.test
  (:refer-clojure :exclude [test])
  (:require [clojure.test :refer :all]
            [leiningen.test :refer :all]
            [leiningen.test.helper :refer [tmp-dir sample-no-aot-project
                                           lein-test-exit-code-project
                                           lein-test-reload-bug-project
                                           sample-reader-cond-project
                                           sample-failing-project
                                           sample-fixture-error-project
                                           with-system-err-str
                                           with-system-out-str]]
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
  (is (= #{:default :integration :int2 :no-custom}
         (set (keys (:test-selectors sample-no-aot-project)))))
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

(deftest test-reader-conditional-tests
  (test sample-reader-cond-project)
  (is (= (ran?) #{:clj-test :cljc-test})))

(deftest test-namespaces-load-in-order
  ;; Issue #2715
  (test lein-test-reload-bug-project))

(deftest test-failure-exit-code
  (is (= 1 (try
             ;; suppress output; there's a lot of bad-looking stuff here
             (with-out-str
               (test lein-test-exit-code-project))
             (catch clojure.lang.ExceptionInfo e
               (:exit-code (ex-data e)))))))

(deftest test-invalid-namespace-argument
  (is (.contains
       (with-system-err-str
         (try
           (test sample-no-aot-project "boom")
           (catch clojure.lang.ExceptionInfo e
             (when-not (:exit-code (ex-data e))
               (throw e)))))
       "java.io.FileNotFoundException: Could not locate")))

(deftest test-file-argument
  (let [file (io/file (first (:test-paths sample-no-aot-project)) "selectors.clj")]
    (test sample-no-aot-project (.getPath file)))
  (is (= (ran?) #{:regular :not-custom :int2 :fixture})))

(deftest test-unreadable-test-fails
  (let [project (project/merge-profiles sample-failing-project
                                        [{:aot ^:replace []
                                          :dependencies ^:replace
                                          [['org.clojure/clojure (clojure-version)]]}])]
    (binding [main/*exit-process?* false]
      (is (= "EOF while reading" (try (test project) false
                                      (catch Exception e
                                        (.getMessage e))))))))

(deftest test-catch-fixture-errors
  (with-system-out-str
    (test sample-fixture-error-project))
  (is (= (ran?) #{:test-a :test-c})))
