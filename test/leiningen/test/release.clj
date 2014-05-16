(ns leiningen.test.release
  (:use [clojure.test]
        [leiningen.release]))

(def invalid-maven-version-values
  [ ["-1"
    {:format :not-recognized,
     :version "-1"}]
    ["derpin"
    {:format :not-recognized
     :version "derpin"}]])

(def valid-maven-version-values
  [["1"
    {:format :major-only,
     :version [1]
     :qualifier nil}]

   ["1-SNAPSHOT"
    {:format :major-only,
     :version [1]
     :qualifier "SNAPSHOT"}]

   ["1-b123"
    {:format :major-only,
     :version [1]
     :qualifier "b123"}]

   ["1.2"
    {:format :major-and-minor,
     :version [1 2]
     :qualifier nil}]

   ["1.2-SNAPSHOT"
    {:format :major-and-minor,
     :version [1 2]
     :qualifier "SNAPSHOT"}]

   ["1.2-b123"
    {:format :major-and-minor,
     :version [1 2]
     :qualifier "b123"}]

   ["1.2.3"
    {:format :major-minor-and-incremental,
     :version [1 2 3]
     :qualifier nil}]

   ["1.2.3-SNAPSHOT"
    {:format :major-minor-and-incremental,
     :version [1 2 3]
     :qualifier "SNAPSHOT"}]

   ["1.2.3-b123"
    {:format :major-minor-and-incremental,
     :version [1 2 3]
     :qualifier "b123"}]

   ["1.2.3-rc1"
    {:format :major-minor-and-incremental,
     :version [1 2 3]
     :qualifier "rc1"}]])

(deftest parse-valid-maven-version
  (doseq [maven-test-data valid-maven-version-values]
    (is (= (parse-maven-version (first maven-test-data))
           (second maven-test-data)))))

(deftest parse-invalid-maven-version
  (doseq [maven-test-data invalid-maven-version-values]
    (is (= (parse-maven-version (first maven-test-data))
           (second maven-test-data)))))

(deftest version-map->string-valid
  (doseq [maven-test-data valid-maven-version-values]
    (is (= (first maven-test-data)
           (version-map->string (second maven-test-data))))))

(deftest version-map->string-invalid
  (doseq [maven-test-data invalid-maven-version-values]
    (is (= (first maven-test-data)
           (version-map->string (second maven-test-data))))))

;; TODO increment-version test
