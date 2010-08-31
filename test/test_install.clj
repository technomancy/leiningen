(ns test-install
  (:use [leiningen.core :only [read-project defproject home-dir]]
        [leiningen.install] :reload-all)
  (:use [clojure.test]
        [clojure.contrib.io :only [delete-file-recursively file]]
        [clojure.contrib.shell :only [with-sh-dir sh]]))

(def local-repo (file (System/getProperty "user.home") ".m2" "repository"))

(def m2-dir (file local-repo "nomnomnom" "nomnomnom" "0.5.0-SNAPSHOT"))

(defonce test-project (read-project "test_projects/sample/project.clj"))

(deftest test-install
  (delete-file-recursively m2-dir true)
  (install test-project)
  (is (not (empty? (.listFiles m2-dir)))))

(def jdom-dir (file local-repo "jdom" "jdom" "1.0"))

(deftest test-standalone-install
  (delete-file-recursively jdom-dir true)
  (install "nomnomnom" "0.5.0-SNAPSHOT")
  (is (not (empty? (.listFiles jdom-dir))))
  (is (.exists (file (home-dir) "bin" "nom"))))
