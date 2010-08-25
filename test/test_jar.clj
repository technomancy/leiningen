(ns test-jar
  (:use [clojure.test]
        [clojure.contrib.io :only [slurp*]]
        [leiningen.core :only [defproject read-project]]
        [leiningen.compile :only [*suppress-err*]]
        [leiningen.jar])
  (:import [java.util.jar JarFile]))

(defproject mock-project "1.0" :main foo.one-two.three-four.bar
  :manifest {"hello" "world"})

(deftest test-manifest
  (let [manifest (manifest-map (make-manifest mock-project))]
    (is (= {"Main-Class" "foo.one_two.three_four.bar", "hello" "world"}
           (select-keys manifest ["hello" "Main-Class"])))))

(def sample-project (binding [*ns* (the-ns 'leiningen.core)]
                      (read-project "test_projects/sample/project.clj")))

(deftest test-jar
  (let [jar-file (JarFile. (jar sample-project))
        manifest (manifest-map (.getManifest jar-file))
        bin (slurp* (.getInputStream jar-file (.getEntry jar-file "bin/nom")))]
    (is (= "bin/nom" (manifest "Leiningen-shell-wrapper")))
    (is (re-find #"org/clojure/clojure/1.1.0-master-SNAPSHOT/" bin))
    (is (re-find #"use 'nom\.nom\.nom\)\(apply -main .command-line-args." bin))
    (is (re-find #"\$HOME/\.m2/repository/rome/rome/0.9/rome-0\.9\.jar" bin))))

(deftest test-no-bin-jar
  (let [jar-file (JarFile. (jar (dissoc sample-project :shell-wrapper)))
        manifest (manifest-map (.getManifest jar-file))]
    (is (nil? (.getEntry jar-file "bin/nom")))
    (is (nil? (manifest "Leiningen-shell-wrapper")))))

(def sample-failing-project
  (binding [*ns* (the-ns 'leiningen.core)]
    (read-project "test_projects/sample_failing/project.clj")))

(deftest test-jar-fails
  (binding [*suppress-err* true]
    (is (not (jar sample-failing-project)))))

(def sample-no-aot-project
  (binding [*ns* (the-ns 'leiningen.core)]
    (read-project "test_projects/sample_no_aot/project.clj")))

(deftest test-no-aot-jar-succeeds
  (binding [*suppress-err* true]
    (is (jar sample-no-aot-project))))
