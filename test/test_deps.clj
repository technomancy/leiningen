(ns test-deps
  (:use [leiningen.core :only [read-project defproject]]
        [leiningen.deps :only [deps]] :reload-all)
  (:use [clojure.test]
        [clojure.contrib.set]
        [clojure.contrib.java-utils :only [file delete-file-recursively]]))

(def test-project (read-project "sample/project.clj"))

(deftest test-deps
  (delete-file-recursively (file (:root test-project) "lib") true)
  (deps test-project)
  (is (subset? #{"jdom-1.0.jar" "tagsoup-1.2.jar"
                 "clojure-1.1.0-alpha-SNAPSHOT.jar" "rome-0.9.jar"}
               (set (map #(.getName %)
                         (.listFiles (file (:root test-project) "lib")))))))
