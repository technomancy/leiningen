(ns leiningen.test.uberjar
  (:require [leiningen.uberjar :refer :all]
            [clojure.test :refer :all]
            [clojure.java.shell :refer [sh]]
            [clojure.xml :as xml]
            [leiningen.test.helper :refer [sample-no-aot-project
                                           uberjar-merging-project
                                           provided-project]])
  (:import (java.io File FileOutputStream)
           (java.util.zip ZipFile)))

(deftest test-uberjar
  (uberjar sample-no-aot-project)
  (let [filename (str "test_projects/sample-no-aot/target/"
                      "nomnomnom-0.5.0-SNAPSHOT-standalone.jar")
        uberjar-file (File. filename)]
    (is (= true (.exists uberjar-file)))
    (when (.exists uberjar-file)
      (let [entries (->> (ZipFile. uberjar-file)
                         .entries
                         enumeration-seq
                         (map (memfn getName))
                         set)]
        (.deleteOnExit uberjar-file)
        (is (entries "nom/nom/nom.clj"))
        (is (entries "org/codehaus/janino/Compiler$1.class"))
        (is (not (some #(re-find #"dummy" %) entries)))))))

(deftest test-uberjar-merge-with
  (uberjar uberjar-merging-project)
  (let [filename (str "test_projects/uberjar-merging/target/"
                      "nomnomnom-0.5.0-SNAPSHOT-standalone.jar")
        uberjar-file (File. filename)]
    (is (= true (.exists uberjar-file)))
    (when (.exists uberjar-file)
      (.deleteOnExit uberjar-file)
      (with-open [zf (ZipFile. uberjar-file)]
        (is (= '{nomnomnom/identity clojure.core/identity
                 mf/i nomnomnom/override
                 mf/s method.fn/static}
               (->> (.getEntry zf "data_readers.clj")
                    (.getInputStream zf)
                    slurp read-string)))))))

(deftest test-components-merger
  (let [file1 (str "test_projects/uberjar-components-merging/components1.xml")
        file2 (str "test_projects/uberjar-components-merging/components2.xml")
        readxml (components-merger 0)
        combine (components-merger 1)
        writexml (components-merger 2)
        combined-xml (combine (readxml file1) (readxml file2))
        expected-xml (xml/parse "test_projects/uberjar-components-merging/expected-components.xml")
        result-file "test_projects/uberjar-components-merging/result-components.xml"
        out-file (FileOutputStream. (File. result-file))]
      (writexml out-file combined-xml)
      (is (= expected-xml (xml/parse result-file)))
    )
    )

;; TODO: this breaks on Java 6
(deftest ^:disabled test-uberjar-provided
  (let [bootclasspath "-Xbootclasspath/a:leiningen-core/lib/clojure-1.4.0.jar"
        filename "test_projects/provided/target/provided-0-standalone.jar"
        _ (uberjar provided-project)]
    (is (= 1 (:exit (sh "java" "-jar" filename))))
    (is (= 0 (:exit (sh "java" bootclasspath "-jar" filename))))))
