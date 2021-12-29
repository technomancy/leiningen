(ns leiningen.core.test.utils
  (:require [leiningen.core.utils :as utils]
            [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]))

(def profiles "./leiningen-core/test/resources/")

(def sample-profile {:user {:plugins '[[lein-pprint "1.1.1"]]}})

(deftest read-profiles
  (testing "Empty profile file"
    (is (nil? (with-redefs [println (constantly nil)]
                (utils/read-file (io/file (str profiles "profiles-empty.clj")))))))
  (testing "Non-empty profile file"
    (is (= (utils/read-file (io/file (str profiles "profiles.clj"))) sample-profile))))

(deftest properties-strip-comments
  (with-open [baos (java.io.ByteArrayOutputStream.)]
    (let [properties (doto (java.util.Properties.)
                       (.setProperty "version" "0.1.0-SNAPSHOT")
                       (.setProperty "groupId" "groupId")
                       (.setProperty "artifactId" "(:name project)"))]
      (.store properties baos "Extra comment")
      (let [str (-> baos
                    str
                    utils/strip-properties-comments)]
        (with-open [input-stream (io/input-stream (.getBytes str))]
          (is (= properties (doto (java.util.Properties.) (.load input-stream)))))))))
