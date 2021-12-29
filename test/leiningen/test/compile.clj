(ns leiningen.test.compile
  (:refer-clojure :exclude [compile])
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [with-sh-dir]]
            [leiningen.compile :refer :all]
            [leiningen.test.helper :refer [sample-project
                                           delete-file-recursively
                                           sample-ordered-aot-project
                                           sample-failing-project
                                           sample-reader-cond-project
                                           tricky-name-project
                                           more-gen-classes-project
                                           with-system-err-str
                                           with-system-out-str]]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]))

(use-fixtures :each (fn [f]
                      (delete-file-recursively
                       (io/file "test_projects" "sample" "target") true)
                      (delete-file-recursively
                       (io/file "test_projects" "sample-failing" "target") true)
                      (binding [main/*info* false]
                        (f))))

(deftest ^:online test-compile
  (with-out-str
    (compile sample-project "nom.nom.nom"))
  (is (.exists (io/file "test_projects" "sample" "target"
                     "classes" "nom" "nom" "nom.class")))
  (with-system-err-str
    (is (thrown? Exception (binding [*err* (java.io.StringWriter.)]
                             (compile sample-failing-project))))))

(deftest ^:online test-compile-all
  (compile sample-project ":all")
  (is (.exists (io/file "test_projects" "sample" "target"
                        "classes" "nom" "nom" "nom.class"))))

(deftest test-compile-order-sorted
  (is (= 0
    (compare
      (vec (compilable-namespaces sample-ordered-aot-project))
      (vec (sort (compilable-namespaces sample-ordered-aot-project)))))))

(deftest test-compile-regex
  (compile more-gen-classes-project "#\"\\.ba.$\"")
  (is (.exists (io/file "test_projects" "more-gen-classes" "target"
                        "classes" "more_gen_classes" "bar.class")))
  (is (.exists (io/file "test_projects" "more-gen-classes" "target"
                        "classes" "more_gen_classes" "baz.class")))
  (is (not (.exists (io/file "test_projects" "more-gen-classes" "target"
                             "classes" "more_gen_classes" "foo.class")))))

(deftest test-compile-cljc
  (compile sample-reader-cond-project)
  (is (.exists (io/file "test_projects" "sample-reader-cond" "target"
                        "classes" "nom" "nom" "clj__init.class")))
  (is (.exists (io/file "test_projects" "sample-reader-cond" "target"
                        "classes" "nom" "nom" "cljc__init.class"))))

(def eip-check (atom false))

(deftest ^:online test-plugin
  (reset! eip-check false)
  (eval/eval-in-project (assoc sample-project
                          :eval-in :leiningen
                          :skip-shutdown-agents true
                          :main nil)
                        `(reset! eip-check true))
  (is @eip-check))

(deftest ^:online test-cleared-transitive-aot
  (compile (assoc sample-project :clean-non-project-classes true) "nom.nom.nom")
  (eval/eval-in-project sample-project '(require 'nom.nom.nom))
  (let [classes (seq (.list (io/file "test_projects" "sample" "target"
                                     "classes" "nom" "nom")))]
    (doseq [r [#"nom\$fn__\d+.class" #"nom\$loading__\d+__auto__.class"
               #"nom\$_main.class" #"nom.class" #"nom__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (io/file "test_projects" "sample" "target"
                             "classes" "sample2" "core.class"))))
  (is (not (.exists (io/file "test_projects" "sample" "target"
                             "classes" "sample2" "alt.class")))))

(deftest ^:online test-cleared-transitive-aot-by-regexes
  (compile (assoc sample-project :clean-non-project-classes [#"core"])
           "nom.nom.check")
  (let [classes (seq (.list (io/file "test_projects" "sample" "target"
                                     "classes" "nom" "nom")))]
    (doseq [r [#"check\$loading__\d+__auto__.class"
               #"check\$_main.class" #"check.class" #"check__init.class"]]
      (is (some (partial re-find r) classes) (format "missing %s" r))))
  (is (not (.exists (io/file "test_projects" "sample" "target"
                             "classes" "sample2" "core.class"))))
  (is (.exists (io/file "test_projects" "sample" "target" "classes"
                        "sample2" "alt__init.class"))))

(deftest ^:online test-injection
  (eval/eval-in-project (assoc sample-project
                          :injections ['(do (ns inject.stuff)
                                            (def beef :hot))])
                        '#'inject.stuff/beef))

;; (deftest test-compile-java-main
;;   (compile dev-deps-project))

(deftest bad-aot-test
  (is (re-find #"does\.not\.exist|does\/not\/exist"
               (with-system-err-str
                 (try
                   (binding [*err* (java.io.StringWriter.)]
                     (compile (assoc sample-project :aot '[does.not.exist])))
                   (catch clojure.lang.ExceptionInfo _))))))

(deftest compilation-specs-tests
  (is (= '[foo bar] (compilation-specs ["foo" "bar"])))
  (is (= [:all] (compilation-specs [":all"]) (compilation-specs [:all])))
  (is (every? #'leiningen.compile/regex?
              (compilation-specs ["#\"foo\"" #"bar" "#\"baz\""])))
  (testing "that regexes are compiled first"
    (let [spec (compilation-specs '[foo #"baz" bar #"quux"])]
      (is (every? symbol? (drop-while #'leiningen.compile/regex? spec))))))
