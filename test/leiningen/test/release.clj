(ns leiningen.test.release
  (:use [clojure.test]
        [clojure.pprint :as pprint]
        [leiningen.release]))

(def invalid-semver-version-values
  [["1.0"
    "1.0"]
    ["derpin"
    "derpin"]])

(def valid-semver-version-values
  [["1.0.0"
   {:major 1
    :minor 0
    :patch 0
    :qualifier nil}
   "1.0.0"
   {:major "2.0.0"
    :minor "1.1.0"
    :patch "1.0.1"}]

   ["1.2.3"
   {:major 1
    :minor 2
    :patch 3
    :qualifier nil}
   "1.2.3"
   {:major "2.0.0"
    :minor "1.3.0"
    :patch "1.2.4"}]

   ["1.2.3-herp"
   {:major 1
    :minor 2
    :patch 3
    :qualifier "herp"}
   "1.2.3"
   {:major "2.0.0"
    :minor "1.3.0"
    :patch "1.2.4"}]

   ["1.0.0-SNAPSHOT"
   {:major 1
    :minor 0
    :patch 0
    :qualifier "SNAPSHOT"}
   "1.0.0"
   {:major "2.0.0"
    :minor "1.1.0"
    :patch "1.0.1"}]])

(deftest test-parse-semver-version
  (testing "Testing semantic version string parsing"
    (doseq [semver-test-data valid-semver-version-values]
      (testing (format "with valid version strings: %s"
                       (first semver-test-data))
        (is (= (parse-semantic-version (first semver-test-data))
               (second semver-test-data)))))

    (testing "with invalid version strings."
      (doseq [semver-test-data invalid-semver-version-values]
        (is (thrown-with-msg?
              Exception #"Unrecognized version string"
              (parse-semantic-version (first semver-test-data))))))))

(deftest version-map->string-valid
  (doseq [semver-test-data valid-semver-version-values]
    (is (= (nth semver-test-data 2)
           (version-map->string (second semver-test-data))))))

(deftest test-increment-version
  (testing "Testing semantic version increment"
    (doseq [semver-test-data valid-semver-version-values]
      (testing (format "with valid version: %s\n"
                       semver-test-data)
        (doseq [[k v] (map identity (nth semver-test-data 3))]
          (testing (format "version-level %s" (name k))
            (is (= v (version-map->string
                       (increment-version
                         (nth semver-test-data 1) k))))))))))
