(ns leiningen.test.uberjar
  (:use [leiningen.uberjar] :reload)
  (:use [clojure.test]
        [clojure.java.shell :only [sh]]
        [leiningen.test.helper :only [sample-no-aot-project provided-project]])
  (:import [java.util.zip ZipFile]))

(deftest test-uberjar
  (let [filename "test_projects/sample_no_aot/target/nomnomnom-0.5.0-SNAPSHOT-standalone.jar"
        _ (uberjar sample-no-aot-project)
        entries (->> (ZipFile. filename)
                    .entries
                    enumeration-seq
                    (map (memfn getName))
                    set)]
    (is (entries "nom/nom/nom.clj"))
    (is (entries "org/codehaus/janino/Compiler$1.class"))
    (is (not (some #(re-find #"dummy" %) entries)))))

(deftest test-uberjar-provided
  (let [-Xbootclasspath "-Xbootclasspath/a:leiningen-core/lib/clojure-1.4.0.jar"
        filename "test_projects/provided/target/provided-0-standalone.jar"
        _ (uberjar provided-project)]
    (is (= 1 (:exit (sh "java" "-jar" filename))))
    (is (= 0 (:exit (sh "java" -Xbootclasspath "-jar" filename))))))
