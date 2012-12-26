(ns leiningen.test.uberjar
  (:require [leiningen.uberjar :refer :all]
            [clojure.test :refer :all]
            [clojure.java.shell :refer [sh]]
            [leiningen.test.helper :refer [sample-no-aot-project
                                           provided-project]])
  (:import (java.util.zip ZipFile)))

(deftest test-uberjar
  (let [filename (str "test_projects/sample_no_aot/target/"
                      "nomnomnom-0.5.0-SNAPSHOT-standalone.jar")
        _ (uberjar sample-no-aot-project)
        entries (->> (ZipFile. filename)
                    .entries
                    enumeration-seq
                    (map (memfn getName))
                    set)]
    (is (entries "nom/nom/nom.clj"))
    (is (entries "org/codehaus/janino/Compiler$1.class"))
    (is (not (some #(re-find #"dummy" %) entries)))))

;; TODO: this breaks on Java 6
(deftest ^:disabled test-uberjar-provided
  (let [bootclasspath "-Xbootclasspath/a:leiningen-core/lib/clojure-1.4.0.jar"
        filename "test_projects/provided/target/provided-0-standalone.jar"
        _ (uberjar provided-project)]
    (is (= 1 (:exit (sh "java" "-jar" filename))))
    (is (= 0 (:exit (sh "java" bootclasspath "-jar" filename))))))
