(ns leiningen.test.jar
  (:use [clojure.test]
        [leiningen.core :only [defproject read-project]]
        [leiningen.jar]
        [leiningen.compile :only [platform-nullsink]]
        [leiningen.test.helper :only [tricky-name-project sample-failing-project
                                      sample-no-aot-project sample-project]])
  (:import [java.util.jar JarFile]))

(def mock-project @(defproject mock-project "1.0"
                     :main foo.one-two.three-four.bar
                     :manifest {"hello" "world"}))

(deftest test-manifest
  (is (= {"Main-Class" "foo.one_two.three_four.bar", "hello" "world"}
         (-> mock-project
             make-manifest
             manifest-map
             (select-keys ["hello" "Main-Class"])))))

(deftest test-jar
  (let [jar-file (JarFile. (jar sample-project))
        manifest (manifest-map (.getManifest jar-file))
        bin (slurp (.getInputStream jar-file (.getEntry jar-file "bin/nom")))
        bat (slurp (.getInputStream jar-file (.getEntry jar-file
                                                        "bin/nom.bat")))]
    (is (= "bin/nom" (manifest "Leiningen-shell-wrapper")))
    (is (re-find #"org/clojure/clojure/1\.1\.0/" bin))
    (is (re-find #"org\\clojure\\clojure\\1\.1\.0" bat))
    (is (re-find #"MAIN=\"nom\.nom\.nom\"" bin))
    (is (re-find #"set MAIN=\"nom\.nom\.nom\"" bat))
    (is (re-find #"use '\$MAIN\)\(apply -main .command-line-args." bin))
    (is (re-find #"use '%MAIN%\)\(apply -main .command-line-args." bat))
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

(deftest test-jar-fails
  (binding [*err* (java.io.PrintWriter. (platform-nullsink))]
    (is (not (jar sample-failing-project)))))

(deftest test-no-aot-jar-succeeds
  (with-out-str
    (is (jar sample-no-aot-project))))

(deftest test-tricky-name
  (let [jar-file (JarFile. (jar tricky-name-project))
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
