(ns test-install
  (:use [leiningen.core :only [read-project defproject]]
        [leiningen.install] :reload-all)
  (:use [clojure.test]
        [clojure.contrib.io :only [delete-file-recursively file]]
        [clojure.contrib.shell :only [with-sh-dir sh]]))

(def m2-dir (file (System/getProperty "user.home") ".m2" "repository"
                  "nomnomnom" "nomnomnom" "0.5.0-SNAPSHOT"))

(defonce test-project (read-project "sample/project.clj"))

(deftest test-install
  (delete-file-recursively m2-dir true)
  (install test-project)
  (is (not (empty? (.listFiles m2-dir)))))
