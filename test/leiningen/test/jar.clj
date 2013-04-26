(ns leiningen.test.jar
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main])
  (:use [clojure.test]
        [leiningen.jar]
        [leiningen.core.eval :only [platform-nullsink]]
        [leiningen.test.helper :only [tricky-name-project sample-failing-project
                                      sample-no-aot-project sample-project
                                      overlapped-sourcepaths-project]])
  (:import (java.util.jar JarFile)))

(def mock-project {:name "mock-project" :version "1.0"
                   :main 'foo.one-two.three-four.bar
                   :manifest {"hello" "world"}})

(deftest test-manifest
  (is (= {"Main-Class" "foo.one_two.three_four.bar", "hello" "world"}
         (-> mock-project
             make-manifest
             manifest-map
             (select-keys ["hello" "Main-Class"])))))

(deftest test-jar-fails
  (binding [*err* (java.io.PrintWriter. (platform-nullsink))]
    (is (thrown? Exception (jar sample-failing-project)))))

(deftest test-no-aot-jar-succeeds
  (with-out-str
    (is (jar sample-no-aot-project))))

(deftest ^:online test-no-deps-jar
  (let [[coord jar-file] (first
                          (jar (dissoc sample-project :dependencies :main)))]
    (is (not (number? jar-file)))
    (is (.exists (io/file jar-file)))
    (is (= coord [:extension "jar"]))))

(deftest overlapped-paths
  (let [info-logs (atom [])]
    (with-redefs [main/info (fn [& args] (swap! info-logs conj args))]
      (let [result (jar overlapped-sourcepaths-project)]
        (is result)
        (is (not-any? #(re-find #"Warning" %) (mapcat identity @info-logs)))))))
