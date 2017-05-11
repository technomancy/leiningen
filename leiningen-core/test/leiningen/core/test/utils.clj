(ns leiningen.core.test.utils
  (:require [leiningen.core.utils :as utils]
            [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]))

(def profiles "./leiningen-core/test/resources/")

(def sample-profile {:user {:plugins '[[lein-pprint "1.1.1"]]}})

(deftest read-profiles
  (testing "Empty profile file"
    (is (nil? (utils/read-file (io/file (str profiles "profiles-empty.clj"))))))
  (testing "Non-empty profile file"
    (is (= (utils/read-file (io/file (str profiles "profiles.clj"))) sample-profile))))
