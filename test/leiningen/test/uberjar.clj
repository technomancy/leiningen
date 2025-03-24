(ns leiningen.test.uberjar
  (:require [leiningen.uberjar :refer :all]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.xml :as xml]
            [leiningen.test.helper :refer [unmemoize
                                           sample-no-aot-project
                                           uberjar-merging-project
                                           data-readers-backwards-compatibility-project
                                           provided-project
                                           managed-deps-project
                                           managed-deps-snapshot-project] :as h])
  (:import (java.io File FileOutputStream)
           (java.util.zip ZipFile)))

(deftest test-uberjar
  (let [project (h/read-test-project "sample-no-aot")
        _ (with-out-str
            (uberjar sample-no-aot-project))
        filename (str "test_projects/sample-no-aot/target/"
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
        (is (not (entries "dev.clj")))
        (is (not (entries "module-info.class")))))))

(deftest test-uberjar-merge-with
  (with-out-str
    (uberjar uberjar-merging-project))
  (let [filename (str "test_projects/uberjar-merging/target/"
                      "nomnomnom-0.5.0-SNAPSHOT-standalone.jar")
        uberjar-file (File. filename)]
    (is (= true (.exists uberjar-file)))
    (when (.exists uberjar-file)
      (.deleteOnExit uberjar-file)
      (with-open [zf (ZipFile. uberjar-file)]
        (is (= '{nomnomnom/identity clojure.core/identity
                 mf/i nomnomnom/override
                 mf/s method.fn/static
                 ordered/set flatland.ordered.set/into-ordered-set
                 ordered/map flatland.ordered.map/ordered-map}
               (->> (.getEntry zf "data_readers.clj")
                    (.getInputStream zf)
                    slurp read-string)))))))

(deftest test-uberjar-data-readers-backwards-compatibility
  (with-out-str
    (uberjar data-readers-backwards-compatibility-project))
  (let [filename (str "test_projects/data-readers-backwards-compatibility/"
                      "target/bug-bug-standalone.jar")
        uberjar-file (File. filename)]
    (is (= true (.exists uberjar-file)))
    (when (.exists uberjar-file)
      (.deleteOnExit uberjar-file)
      (with-open [zf (ZipFile. uberjar-file)]
        (let [contents (->> (.getEntry zf "data_readers.clj")
                            (.getInputStream zf)
                            slurp)]
          (is (.startsWith contents "{")) ;; not a namespaced map
          (is (= '{ordered/set flatland.ordered.set/into-ordered-set
                   ordered/map flatland.ordered.map/ordered-map}
                 (read-string contents))))))))

(deftest test-components-merger
  (let [file1 (io/input-stream "test_projects/uberjar-components-merging/components1.xml")
        file2 (io/input-stream "test_projects/uberjar-components-merging/components2.xml")
        [read-xml combine write-xml] components-merger
        combined-xml (combine (read-xml file1) (read-xml file2))
        expected-xml (xml/parse (io/input-stream
                                 "test_projects/uberjar-components-merging/expected-components.xml")
                                #'leiningen.uberjar/startparse)
        result-file "test_projects/uberjar-components-merging/result-components.xml"
        out-file (FileOutputStream. (File. result-file))]
      (write-xml out-file combined-xml)
      (is (= expected-xml (xml/parse (io/input-stream result-file)
                                     #'leiningen.uberjar/startparse)))
      (io/delete-file result-file true)))

;; TODO: this breaks on Java 6
(deftest ^:disabled test-uberjar-provided
  (let [bootclasspath "-Xbootclasspath/a:leiningen-core/lib/clojure-1.4.0.jar"
        filename "test_projects/provided/target/provided-0-standalone.jar"]
    (with-out-str
      (uberjar provided-project))
    (is (= 1 (:exit (sh "java" "-jar" filename))))
    (is (= 0 (:exit (sh "java" bootclasspath "-jar" filename))))))

(deftest test-uberjar-managed-dependencies
  (unmemoize #'leiningen.core.classpath/get-dependencies-memoized
             #'leiningen.core.classpath/get-dependencies*)
  (doseq [[proj jarfile] [[managed-deps-snapshot-project
                           (str "test_projects/managed-deps-snapshot/target/"
                                "mgmt-0.99.0-SNAPSHOT-standalone.jar")]
                          [managed-deps-project
                           (str "test_projects/managed-deps/target/"
                                "mgmt-0.99.0-standalone.jar")]]]
    (with-out-str
      (uberjar proj))
    (let [uberjar-file (File. jarfile)]
      (is (= true (.exists uberjar-file))
          (format "File '%s' does not exist!" uberjar-file)))))
