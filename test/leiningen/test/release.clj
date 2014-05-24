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
   {:major "2.0.0-SNAPSHOT"
    :minor "1.1.0-SNAPSHOT"
    :patch "1.0.1-SNAPSHOT"}]

   ["1.2.3"
   {:major 1
    :minor 2
    :patch 3
    :qualifier nil}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.3.0-SNAPSHOT"
    :patch "1.2.4-SNAPSHOT"}]

   ["1.2.3-herp"
   {:major 1
    :minor 2
    :patch 3
    :qualifier "herp"}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.3.0-SNAPSHOT"
    :patch "1.2.4-SNAPSHOT"
    :release "1.2.3"}]

   ["1.0.0-SNAPSHOT"
   {:major 1
    :minor 0
    :patch 0
    :qualifier "SNAPSHOT"}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.1.0-SNAPSHOT"
    :patch "1.0.1-SNAPSHOT"
    :release "1.0.0"}]])

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
  (doseq [[string parsed bumps] valid-semver-version-values]
    (is (= string (version-map->string parsed)))
    (doseq [[level string] bumps]
      (is (= (merge {:qualifier nil} (bump-version level parsed))
             (parse-semantic-version string))))))
