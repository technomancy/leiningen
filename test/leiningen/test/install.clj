(ns leiningen.test.install
  (:require [leiningen.core.user :as user]
            [leiningen.core.main :as main]
            [clojure.test :refer :all]
            [leiningen.install :refer :all]
            [leiningen.test.helper :as helper]
            [clojure.java.io :as io]))

(deftest ^:online test-install
  (helper/unmemoize #'leiningen.core.classpath/get-dependencies-memoized
                    #'leiningen.core.classpath/get-dependencies*)
  (helper/delete-file-recursively (helper/m2-dir "nomnomnom" "0.5.0-SNAPSHOT") true)
  (with-out-str
    (binding [main/*info* false]
      (install helper/sample-project)))
  (is (not (empty? (.listFiles (helper/m2-dir "nomnomnom" "0.5.0-SNAPSHOT"))))))

(def tricky-m2-dir (io/file helper/local-repo "org" "domain" "tricky-name" "1.0"))

(deftest ^:online test-tricky-name-install
  (helper/delete-file-recursively tricky-m2-dir true)
  (with-out-str
    (install helper/tricky-name-project))
  (is (not (empty? (.listFiles tricky-m2-dir)))))
