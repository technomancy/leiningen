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

;; (deftest test-compile
;;   (is (zero? (compile (make-project "test_projects/sample"))))
;;   (is (.exists (file "test_projects" "sample"
;;                      "classes" "nom" "nom" "nom.class")))
;;   (is (pos? (compile (make-project "test_projects/sample_failing")))))

;; (deftest test-plugin
;;   (is (= (eval-in-project (assoc (make-project "test_projects/sample")
;;                             :eval-in-leiningen true
;;                             :main nil)
;;                           '(do (require 'leiningen.compile)
;;                                :compiled))
;;          :compiled)))

(deftest test-cleared-transitive-aot
  (is (zero? (compile (make-project "test_projects/sample"))))
  (let [classes (seq (.list (file "test_projects" "sample"
                                  "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto____\d+.class"
               #"nom\$_main__\d+.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (file "test_projects" "sample"
                          "classes" "sample2" "core.class")))))
