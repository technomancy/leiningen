(ns leiningen.test.install
  (:require [leiningen.core.user :as user])
  (:use [clojure.test]
        [leiningen.install]
        [leiningen.test.helper]
        [clojure.java.io :only [file]]))

(deftest ^:online test-install
  (unmemoize #'leiningen.core.classpath/get-dependencies-memoized
             #'leiningen.core.classpath/get-dependencies*)
  (delete-file-recursively (m2-dir "nomnomnom" "0.5.0-SNAPSHOT") true)
  (install sample-project)
  (is (not (empty? (.listFiles (m2-dir "nomnomnom" "0.5.0-SNAPSHOT"))))))

(def tricky-m2-dir (file local-repo "org" "domain" "tricky-name" "1.0"))

(deftest ^:online test-tricky-name-install
  (delete-file-recursively tricky-m2-dir true)
  (install tricky-name-project)
  (is (not (empty? (.listFiles tricky-m2-dir)))))
