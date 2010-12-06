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

(defonce test-project-tricky-name (read-project "test_projects/tricky-name/project.clj"))
(def tricky-m2-dir (file local-repo "org" "domain" "tricky-name" "1.0"))
(def tricky-unix-shell-wrapper (file (home-dir) "bin" "tricky-name"))
(def tricky-windows-shell-wrapper (file (home-dir) "bin" "tricky-name.bat"))
(defn delete-tricky-shell-wrappers []
  (.delete tricky-unix-shell-wrapper)
  (.delete tricky-windows-shell-wrapper))

(deftest test-tricky-name-install
  (delete-file-recursively tricky-m2-dir true)
  (delete-shell-wrappers)
  (install test-project-tricky-name)
  (install "org.domain/tricky-name" "1.0")
  (is (not (empty? (.listFiles tricky-m2-dir))))
  (is (.exists tricky-unix-shell-wrapper))
  (if (= :windows (get-os))
    (is (.exists tricky-windows-shell-wrapper))
    (is (not (.exists tricky-windows-shell-wrapper)))))
