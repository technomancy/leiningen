(ns leiningen.test.compile
  (:refer-clojure :exclude [compile])
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [clojure.java.shell :only [with-sh-dir sh]]
        [leiningen.compile]
        [leiningen.core :only [read-project]]
        [leiningen.test.helper :only [sample-project sample-failing-project
                                      tricky-name-project]]
        [leiningen.util.file :only [delete-file-recursively]]))

(use-fixtures :each (fn [f]
                      (delete-file-recursively
                       (file "test_projects" "sample" "classes") true)
                      (delete-file-recursively
                       (file "test_projects" "sample_failing" "classes") true)
                      (f)))

(deftest test-compile
  (is (zero? (compile sample-project)))
  (is (.exists (file "test_projects" "sample"
                     "classes" "nom" "nom" "nom.class")))
  (is (pos? (compile sample-failing-project))))

(deftest test-plugin
  (is (zero? (eval-in-project (assoc sample-project
                                :eval-in-leiningen true
                                :main nil)
                              '(do (require 'leiningen.compile)
                                   :compiled)))))

(deftest test-cleared-transitive-aot
  (is (zero? (compile (assoc sample-project
                        :clean-non-project-classes true))))
  (let [classes (seq (.list (file "test_projects" "sample"
                                  "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto____\d+.class"
               #"nom\$_main__\d+.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (file "test_projects" "sample"
                          "classes" "sample2" "core.class"))))
  (is (not (.exists (file "test_projects" "sample"
                          "classes" "sample2" "alt.class")))))

(deftest test-cleared-transitive-aot-by-regexes
  (is (zero? (compile (assoc sample-project
                        :clean-non-project-classes [#"core"]))))
  (let [classes (seq (.list (file "test_projects" "sample"
                                  "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto____\d+.class"
               #"nom\$_main__\d+.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (file "test_projects" "sample"
                          "classes" "sample2" "core.class"))))
  (is (.exists (file "test_projects" "sample" "classes"
                     "sample2" "alt__init.class"))))

(deftest test-skip-aot-on-main
  (delete-file-recursively (file (:root tricky-name-project) "classes") :silent)
  (is (zero? (compile tricky-name-project)))
  (is (empty? (.list (file (:root tricky-name-project) "classes")))))

(deftest test-injection
  (is (zero? (eval-in-project sample-project
                              '#'leiningen.util.injected/add-hook))))
