(ns leiningen.test.compile
  (:refer-clojure :exclude [compile])
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [clojure.java.shell :only [with-sh-dir]]
        [leiningen.compile]
        [leiningen.test.helper :only [sample-project delete-file-recursively
                                      sample-failing-project
                                      tricky-name-project]])
  (:require [leiningen.core.eval :as eval]))

(use-fixtures :each (fn [f]
                      (delete-file-recursively
                       (file "test_projects" "sample" "target") true)
                      (delete-file-recursively
                       (file "test_projects" "sample_failing" "target") true)
                      (f)))

(deftest test-compile
  (is (zero? (compile sample-project)))
  (is (.exists (file "test_projects" "sample" "target"
                     "classes" "nom" "nom" "nom.class")))
  (is (pos? (compile sample-failing-project))))

(deftest test-compile-all
  (is (zero? (compile sample-project ":all")))
  (is (.exists (file "test_projects" "sample" "target"
                     "classes" "nom" "nom" "nom.class"))))

(def eip-check (atom false))

(deftest test-plugin
  (reset! eip-check false)
  (is (zero? (eval/eval-in-project (assoc sample-project
                                     :eval-in :leiningen
                                     :skip-shutdown-agents true
                                     :main nil)
                                   `(reset! eip-check true))))
  (is @eip-check))

(deftest ^:post-preview test-cleared-transitive-aot
  (is (zero? (compile (assoc sample-project
                        :clean-non-project-classes true))))
  (is (zero? (eval/eval-in-project sample-project '(require 'nom.nom.nom)))
      "can't load after compiling")
  (let [classes (seq (.list (file "test_projects" "sample" "target"
                                  "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto____\d+.class"
               #"nom\$_main__\d+.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (file "test_projects" "sample" "target"
                          "classes" "sample2" "core.class"))))
  (is (not (.exists (file "test_projects" "sample" "target"
                          "classes" "sample2" "alt.class")))))

(deftest ^:post-preview test-cleared-transitive-aot-by-regexes
  (is (zero? (compile (assoc sample-project
                        :clean-non-project-classes [#"core"]))))
  (let [classes (seq (.list (file "test_projects" "sample" "target"
                                  "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto____\d+.class"
               #"nom\$_main__\d+.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (file "test_projects" "sample" "target"
                          "classes" "sample2" "core.class"))))
  (is (.exists (file "test_projects" "sample" "target" "classes"
                     "sample2" "alt__init.class"))))

(deftest test-skip-aot-on-main
  (delete-file-recursively (:compile-path tricky-name-project) :silent)
  (is (zero? (compile tricky-name-project)))
  (is (empty? (.list (file (:compile-path tricky-name-project))))))

(deftest test-injection
  (is (zero? (eval/eval-in-project sample-project
                                   '#'leiningen.core.injected/add-hook))))

;; (deftest test-compile-java-main
;;   (is (zero? (compile dev-deps-project))))
