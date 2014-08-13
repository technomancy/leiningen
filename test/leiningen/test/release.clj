(ns leiningen.test.release
  (:require [clojure.test :refer :all]
            [clojure.pprint :as pprint]
            [leiningen.release :refer :all]))

(def invalid-semver-version-values
  [["1.0" "1.0"]
    ["derpin" "derpin"]])

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
    :release "1.0.0"
    :alpha "1.0.0-alpha1"
    :beta "1.0.0-beta1"
    :rc "1.0.0-RC1"}]

   ["1.0.0-alpha1"
   {:major 1
    :minor 0
    :patch 0
    :qualifier "alpha1"}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.1.0-SNAPSHOT"
    :patch "1.0.1-SNAPSHOT"
    :release "1.0.0"
    :alpha "1.0.0-alpha2"
    :beta "1.0.0-beta1"
    :rc "1.0.0-RC1"}]])

(deftest test-string->semantic-version
  (testing "Testing semantic version string parsing"
    (doseq [[args expected] valid-semver-version-values]
      (testing (format "with valid version strings: %s" args)
        (is (= (string->semantic-version args) expected))))

    (testing "with invalid version strings."
      (doseq [[semver-test-data] invalid-semver-version-values]
        (is (nil? (string->semantic-version semver-test-data)))))))

(deftest test-parse-semver-version
  (testing "Testing semantic version string parsing"
    (doseq [[args expected] valid-semver-version-values]
      (testing (format "with valid version strings: %s" args)
        (is (= (parse-semantic-version args) expected))))

    (testing "with invalid version strings."
      (doseq [[semver-test-data] invalid-semver-version-values]
        (is (thrown-with-msg? Exception #"Unrecognized version string"
                              (binding [leiningen.core.main/*exit-process?* false
                                        *err* (java.io.StringWriter.)]
                                (parse-semantic-version semver-test-data))))))))

(deftest version-map->string-valid
  (doseq [[string parsed bumps] valid-semver-version-values]
    (is (= string (version-map->string parsed)))
    (doseq [[level string] bumps]
      (is (= (merge {:qualifier nil} (bump-version-map parsed level))
             (parse-semantic-version string))))))
