(ns test-jar
  (:use [clojure.test]
        [leiningen.core :only [defproject read-project]]
        [leiningen.jar]
        [leiningen.compile :only [platform-nullsink]])
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
        bin (slurp (.getInputStream jar-file (.getEntry jar-file "bin/nom")))
        bat (slurp (.getInputStream jar-file (.getEntry jar-file
                                                        "bin/nom.bat")))]
    (is (= "bin/nom" (manifest "Leiningen-shell-wrapper")))
    (is (re-find #"org/clojure/clojure/1\.1\.0/" bin))
    (is (re-find #"org\\clojure\\clojure\\1\.1\.0" bat))
    (is (re-find #"use 'nom\.nom\.nom\)\(apply -main .command-line-args." bin))
    (is (re-find #"use 'nom\.nom\.nom\)\(apply -main .command-line-args." bat))
    (is (re-find #"\$HOME/\.m2/repository/rome/rome/0\.9/rome-0\.9\.jar" bin))
    (is (re-find
         #"%USERPROFILE%\\\.m2\\repository\\rome\\rome\\0\.9\\rome-0\.9\.jar"
         bat))))

(deftest test-no-bin-jar
  (let [jar-file (JarFile. (jar (dissoc sample-project :shell-wrapper)))
        manifest (manifest-map (.getManifest jar-file))]
    (is (nil? (.getEntry jar-file "bin/nom")))
    (is (nil? (.getEntry jar-file "bin/nom.bat")))
    (is (nil? (manifest "Leiningen-shell-wrapper")))))

(def sample-failing-project
  (binding [*ns* (the-ns 'leiningen.core)]
    (read-project "test_projects/sample_failing/project.clj")))

(deftest test-jar-fails
  (binding [*err* (java.io.PrintWriter. (platform-nullsink))]
    (is (not (jar sample-failing-project)))))

(def sample-no-aot-project
  (binding [*ns* (the-ns 'leiningen.core)]
    (read-project "test_projects/sample_no_aot/project.clj")))

(deftest test-no-aot-jar-succeeds
  (with-out-str
    (is (jar sample-no-aot-project))))

(def tricky-name
  (binding [*ns* (the-ns 'leiningen.core)]
    (read-project "test_projects/tricky-name/project.clj")))

(deftest test-tricky-name
  (let [jar-file (JarFile. (jar tricky-name))
        manifest (manifest-map (.getManifest jar-file))
        bin (slurp (.getInputStream
                    jar-file (.getEntry jar-file "bin/tricky-name")))
        bat (slurp (.getInputStream
                    jar-file (.getEntry jar-file "bin/tricky-name.bat")))]
    (is (= "bin/tricky-name" (manifest "Leiningen-shell-wrapper")))
    (is (re-find #"org/domain/tricky-name/1\.0/tricky-name-1\.0\.jar" bin))
    (is (re-find #"\\domain\\tricky-name\\1\.0\\tricky-name-1\.0\.jar" bat))))

(deftest test-no-deps-jar
  (let [jar-file (jar (dissoc sample-project :dependencies :dev-dependencies))]
    (is (.exists (java.io.File. jar-file)))))
