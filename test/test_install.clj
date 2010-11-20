(ns test-install
  (:use [leiningen.core :only [read-project defproject home-dir]]
        [leiningen.compile :only [get-os]]
        [leiningen.install] :reload)
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.util.file :only [delete-file-recursively]]))

(def local-repo (file (System/getProperty "user.home") ".m2" "repository"))

(def m2-dir (file local-repo "nomnomnom" "nomnomnom" "0.5.0-SNAPSHOT"))

(def unix-shell-wrapper (file (home-dir) "bin" "nom"))
(def windows-shell-wrapper (file (home-dir) "bin" "nom.bat"))

(defonce test-project (read-project "test_projects/sample/project.clj"))

(defn delete-shell-wrappers []
  (.delete unix-shell-wrapper)
  (.delete windows-shell-wrapper))

(deftest test-install
  (delete-file-recursively m2-dir true)
  (delete-shell-wrappers)
  (install test-project)
  (is (not (empty? (.listFiles m2-dir))))
  (is (.exists unix-shell-wrapper))
  (if (= :windows (get-os))
    (is (.exists windows-shell-wrapper))
    (is (not (.exists windows-shell-wrapper)))))

(def jdom-dir (file local-repo "jdom" "jdom" "1.0"))

(deftest test-standalone-install
  (delete-file-recursively jdom-dir true)
  (delete-shell-wrappers)
  (install "nomnomnom" "0.5.0-SNAPSHOT")
  (is (not (empty? (.listFiles jdom-dir))))
  (is (.exists unix-shell-wrapper)))
