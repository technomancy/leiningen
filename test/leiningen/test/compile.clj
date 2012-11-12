(ns leiningen.test.compile
  (:refer-clojure :exclude [compile])
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [clojure.java.shell :only [with-sh-dir]]
        [leiningen.compile]
        [leiningen.test.helper :only [sample-project delete-file-recursively
                                      sample-failing-project
                                      tricky-name-project]])
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]))

(use-fixtures :each (fn [f]
                      (delete-file-recursively
                       (file "test_projects" "sample" "target") true)
                      (delete-file-recursively
                       (file "test_projects" "sample_failing" "target") true)
                      (f)))

(deftest test-compile
  (compile sample-project "nom.nom.nom")
  (is (.exists (file "test_projects" "sample" "target"
                     "classes" "nom" "nom" "nom.class")))
  (is (thrown? Exception (compile sample-failing-project))))

(deftest test-compile-all
  (compile sample-project ":all")
  (is (.exists (file "test_projects" "sample" "target"
                     "classes" "nom" "nom" "nom.class"))))

(def eip-check (atom false))

(deftest test-plugin
  (reset! eip-check false)
  (eval/eval-in-project (assoc sample-project
                          :eval-in :leiningen
                          :skip-shutdown-agents true
                          :main nil)
                        `(reset! eip-check true))
  (is @eip-check))

(deftest test-cleared-transitive-aot
  (compile (assoc sample-project :clean-non-project-classes true) "nom.nom.nom")
  (eval/eval-in-project sample-project '(require 'nom.nom.nom))
  (let [classes (seq (.list (file "test_projects" "sample" "target"
                                  "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto__.class"
               #"nom\$_main.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (file "test_projects" "sample" "target"
                          "classes" "sample2" "core.class"))))
  (is (not (.exists (file "test_projects" "sample" "target"
                          "classes" "sample2" "alt.class")))))

(deftest test-cleared-transitive-aot-by-regexes
  (compile (assoc sample-project :clean-non-project-classes [#"core"])
           "nom.nom.nom")
  (let [classes (seq (.list (file "test_projects" "sample" "target"
                                  "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto__.class"
               #"nom\$_main.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (file "test_projects" "sample" "target"
                          "classes" "sample2" "core.class"))))
  (is (.exists (file "test_projects" "sample" "target" "classes"
                     "sample2" "alt__init.class"))))

(deftest test-injection
  (eval/eval-in-project (assoc sample-project
                          :injections ['(do (ns inject.stuff)
                                            (def beef :hot))])
                        '#'inject.stuff/beef))

;; (deftest test-compile-java-main
;;   (compile dev-deps-project))
