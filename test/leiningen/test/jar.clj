(ns leiningen.test.jar
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main])
  (:use [clojure.test]
        [leiningen.jar]
        [leiningen.core.eval :only [platform-nullsink]]
        [leiningen.test.helper :only [tricky-name-project sample-failing-project
                                      sample-no-aot-project sample-project
                                      overlapped-sourcepaths-project
                                      with-resources-project walkzip]])
  (:import (java.util.jar JarFile)))

(def long-line
  (apply str (repeat 10000 "a")))

(def mock-project {:name "mock-project" :version "1.0"
                   :main 'foo.one-two.three-four.bar
                   :manifest {"hello" "world"
                              "long-line" long-line}})

(deftest test-manifest
  (let [mm (-> mock-project
               make-manifest
               manifest-map)]
    (is (= {"Main-Class" "foo.one_two.three_four.bar", "hello" "world"}
           (select-keys mm ["hello" "Main-Class"])))
    (is (= #{"Manifest-Version" "Main-Class" "hello" "Created-By" "Built-By"
             "Build-Jdk" "long-line"}
           (-> mm keys set)))
    (is (= (get mm "long-line") long-line))))

(deftest test-jar-fails
  (binding [*err* (java.io.PrintWriter. (platform-nullsink))]
    (is (thrown? Exception (jar sample-failing-project)))))

(deftest test-directory-entries-added-to-jar
  (with-out-str
    (let [jar (first (vals (jar with-resources-project)))
          entry-names (set (walkzip jar #(.getName %)))]
      (is (entry-names "nested/dir/"))
      (is (not (some #(.startsWith % "/") entry-names))))))

(deftest test-no-aot-jar-succeeds
  (with-out-str
    (is (jar sample-no-aot-project))))

(deftest ^:online test-no-deps-jar
  (let [[coord jar-file] (first
                          (jar (dissoc sample-project :dependencies :main)))]
    (is (.exists (io/file jar-file)))
    (is (= coord [:extension "jar"]))))

(deftest overlapped-paths
  (let [info-logs (atom [])]
    (with-redefs [main/info (fn [& args] (swap! info-logs conj args))]
      (let [result (jar overlapped-sourcepaths-project)]
        (is result)
        (is (not-any? #(re-find #"Warning" %) (mapcat identity @info-logs)))))))
