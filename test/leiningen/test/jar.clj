(ns leiningen.test.jar
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [leiningen.core.utils :refer [platform-nullsink]]
            [leiningen.test.helper :as helper])
  (:use [clojure.test]
        [leiningen.jar])
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
    (is (thrown? Exception (jar helper/sample-failing-project)))))

(deftest test-directory-entries-added-to-jar
  (with-out-str
    (let [jar (first (vals (jar helper/with-resources-project)))
          entry-names (set (helper/walkzip jar #(.getName %)))]
      (is (entry-names "nested/dir/"))
      (is (not (some #(.startsWith % "/") entry-names))))))

(deftest test-profile-added-to-jar
  (with-out-str
    (let [project (-> helper/with-resources-project
                      (project/add-profiles
                       {:test-jar {:resource-paths ^:replace []}})
                      (project/merge-profiles [:test-jar]))
          jar (first (vals (jar project)))
          entry-names (set (helper/walkzip jar #(.getName %)))]
      (is (not (entry-names "nested/dir/sample.txt"))))))

(deftest test-no-aot-jar-succeeds
  (with-out-str
    (is (jar helper/sample-no-aot-project))))

(deftest ^:online test-no-deps-jar
  (let [[coord jar-file] (first
                          (jar (dissoc helper/sample-project
                                       :dependencies :main)))]
    (is (.exists (io/file jar-file)))
    (is (= coord [:extension "jar"]))))

(deftest overlapped-paths
  (let [info-logs (atom [])]
    (with-redefs [main/info (fn [& args] (swap! info-logs conj args))]
      (let [result (jar helper/overlapped-sourcepaths-project)]
        (is result)
        (is (not-any? #(re-find #"Warning" %) (mapcat identity @info-logs)))))))

(deftest test-write-jar
  (testing (str "Confirm that a warning is output when the Main-Class is not "
                "part of the output jar file")
    (let [out-str (with-out-str
                    (write-jar mock-project
                               "/dev/null"
                               [{:type :bytes
                                 :path "foo/one_two.class"
                                 :bytes ""}
                                {:type :bytes
                                 :path "foo/one_two_too.class"
                                 :bytes ""}]))]
      (is (.contains out-str
                     "Warning: The Main-Class specified does not exist"))))

  (testing (str "Confirm that a warning is NOT output when the Main-Class is "
                "not part of the output jar file")
    (let [out-str (with-out-str
                    (write-jar mock-project
                               "/dev/null"
                               [{:type :bytes
                                 :path "foo/one_two.class"
                                 :bytes ""}
                                {:type :bytes
                                 :path "foo/one_two_too.class"
                                 :bytes ""}
                                {:type :bytes
                                 :path "foo/one_two/three_four/bar.class"
                                 :bytes ""}]))]
      (is (not (.contains out-str
                          "Warning: The Main-Class specified does not exist"))))))
