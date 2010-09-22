(ns test-compile
  (:refer-clojure :exclude [compile])
  (:use [leiningen.compile] :reload)
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [clojure.java.shell :only [with-sh-dir sh]]
        [leiningen.core :only [read-project]]
        [leiningen.util.file :only [delete-file-recursively]]))

(use-fixtures :each (fn [f]
                      (delete-file-recursively
                       (file "test_projects" "sample" "classes") true)
                      (delete-file-recursively
                       (file "test_projects" "sample_failing" "classes") true)
                      (f)))

(defn make-project [root]
  (binding [*ns* (find-ns 'leiningen.core)]
    (read-project (.getAbsolutePath (file root "project.clj")))))

(deftest test-compile
  (is (zero? (compile (make-project "test_projects/sample"))))
  (is (.exists (file "test_projects" "sample"
                     "classes" "nom" "nom" "nom.class")))
  (is (pos? (compile (make-project "test_projects/sample_failing")))))
