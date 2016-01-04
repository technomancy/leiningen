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
    :qualifier nil
    :snapshot nil}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.1.0-SNAPSHOT"
    :patch "1.0.1-SNAPSHOT"
    :release "1.0.0"
    :alpha "1.0.0-alpha1-SNAPSHOT"
    :beta "1.0.0-beta1-SNAPSHOT"
    :rc "1.0.0-RC1-SNAPSHOT"
    :qualifier "1.0.0-1-SNAPSHOT"}]

   ["1.2.3"
   {:major 1
    :minor 2
    :patch 3
    :qualifier nil
    :snapshot nil}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.3.0-SNAPSHOT"
    :patch "1.2.4-SNAPSHOT"
    :release "1.2.3"
    :alpha "1.2.3-alpha1-SNAPSHOT"
    :beta "1.2.3-beta1-SNAPSHOT"
    :rc "1.2.3-RC1-SNAPSHOT"
    :qualifier "1.2.3-1-SNAPSHOT"}]

   ["1.2.3-herp"
   {:major 1
    :minor 2
    :patch 3
    :qualifier "herp"
    :snapshot nil}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.3.0-SNAPSHOT"
    :patch "1.2.4-SNAPSHOT"
    :release "1.2.3"
    :alpha "1.2.3-alpha1-SNAPSHOT"
    :beta "1.2.3-beta1-SNAPSHOT"
    :rc "1.2.3-RC1-SNAPSHOT"
    :qualifier "1.2.3-herp1-SNAPSHOT"}]

   ["1.0.0-SNAPSHOT"
   {:major 1
    :minor 0
    :patch 0
    :qualifier nil
    :snapshot "SNAPSHOT"}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.1.0-SNAPSHOT"
    :patch "1.0.1-SNAPSHOT"
    :release "1.0.0"
    :alpha "1.0.0-alpha1-SNAPSHOT"
    :beta "1.0.0-beta1-SNAPSHOT"
    :rc "1.0.0-RC1-SNAPSHOT"
    :qualifier "1.0.0-1-SNAPSHOT"}]

   ["1.0.0-alpha1"
   {:major 1
    :minor 0
    :patch 0
    :qualifier "alpha1"
    :snapshot nil}
   {:major "2.0.0-SNAPSHOT"
    :minor "1.1.0-SNAPSHOT"
    :patch "1.0.1-SNAPSHOT"
    :release "1.0.0"
    :alpha "1.0.0-alpha2-SNAPSHOT"
    :beta "1.0.0-beta1-SNAPSHOT"
    :rc "1.0.0-RC1-SNAPSHOT"
    :qualifier "1.0.0-alpha2-SNAPSHOT"}]

   ["1.0.0-alpha1-SNAPSHOT"
    {:major 1
     :minor 0
     :patch 0
     :qualifier "alpha1"
     :snapshot "SNAPSHOT"}
    {:major "2.0.0-SNAPSHOT"
     :minor "1.1.0-SNAPSHOT"
     :patch "1.0.1-SNAPSHOT"
     :release "1.0.0-alpha1"
     :alpha "1.0.0-alpha2-SNAPSHOT"
     :beta "1.0.0-beta1-SNAPSHOT"
     :rc "1.0.0-RC1-SNAPSHOT"
     :qualifier "1.0.0-alpha2-SNAPSHOT"}]

   ["1.0.0-beta1"
    {:major 1
     :minor 0
     :patch 0
     :qualifier "beta1"
     :snapshot nil}
    {:major "2.0.0-SNAPSHOT"
     :minor "1.1.0-SNAPSHOT"
     :patch "1.0.1-SNAPSHOT"
     :release "1.0.0"
     :alpha "1.0.0-alpha1-SNAPSHOT"
     :beta "1.0.0-beta2-SNAPSHOT"
     :rc "1.0.0-RC1-SNAPSHOT"
     :qualifier "1.0.0-beta2-SNAPSHOT"}]

   ["1.0.0-RC2-SNAPSHOT"
    {:major 1
     :minor 0
     :patch 0
     :qualifier "RC2"
     :snapshot "SNAPSHOT"}
    {:major "2.0.0-SNAPSHOT"
     :minor "1.1.0-SNAPSHOT"
     :patch "1.0.1-SNAPSHOT"
     :release "1.0.0-RC2"
     :alpha "1.0.0-alpha1-SNAPSHOT"
     :beta "1.0.0-beta1-SNAPSHOT"
     :rc "1.0.0-RC3-SNAPSHOT"
     :qualifier "1.0.0-RC3-SNAPSHOT"}]

   ["1.2.3-herp2"
    {:major 1
     :minor 2
     :patch 3
     :qualifier "herp2"
     :snapshot nil}
    {:major "2.0.0-SNAPSHOT"
     :minor "1.3.0-SNAPSHOT"
     :patch "1.2.4-SNAPSHOT"
     :release "1.2.3"
     :alpha "1.2.3-alpha1-SNAPSHOT"
     :beta "1.2.3-beta1-SNAPSHOT"
     :rc "1.2.3-RC1-SNAPSHOT"
     :qualifier "1.2.3-herp3-SNAPSHOT"}]

   ["1.2.3-25"
    {:major 1
     :minor 2
     :patch 3
     :qualifier "25"
     :snapshot nil}
    {:major "2.0.0-SNAPSHOT"
     :minor "1.3.0-SNAPSHOT"
     :patch "1.2.4-SNAPSHOT"
     :release "1.2.3"
     :alpha "1.2.3-alpha1-SNAPSHOT"
     :beta "1.2.3-beta1-SNAPSHOT"
     :rc "1.2.3-RC1-SNAPSHOT"
     :qualifier "1.2.3-26-SNAPSHOT"}]])

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
    (doseq [[level expected-bumped-string] bumps]
      (let [bumped (bump-version-map parsed level)]
        (is (= bumped (parse-semantic-version expected-bumped-string)))
        (is (= expected-bumped-string (version-map->string bumped)))))))