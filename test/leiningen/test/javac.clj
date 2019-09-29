(ns leiningen.test.javac
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.javac :only [javac normalize-javac-options]]
        [leiningen.test.helper :only [delete-file-recursively
                                      #_dev-deps-project]]))

(deftest test-javac-options-normalization
  (testing "that Leiningen 2 style options are returned unmodified"
    (are [arg] (= arg (normalize-javac-options arg))
      ["-target" "1.6" "-source" "1.6"]
      ["-deprecation" "-g"]))
  (testing "conversion of Leiningen 1 style options that are supported"
    (are [old new] (= new (normalize-javac-options old))
      {:debug false}                ["-g:none"]
      {:debug "off"}                ["-g:none"]
         ;; overriden by :compile-path
      {:destdir "clazzez"}          []
      {:encoding "utf8"}            ["-encoding" "utf8"]
      {:debugLevel "source,lines"}  ["-g:source,lines"]))
  (testing "conversion of multiple Leiningen 1 style options"
    ;; Cannot assume argument order from hash maps
    (are [old new] (= new
                      (apply hash-map (normalize-javac-options old)))
      {:source "1.5" :target "1.5"} {"-target" "1.5" "-source" "1.5"}
      {:source 1.5   "target" 1.5}  {"-target" "1.5" "-source" "1.5"})))

(deftest ^:disabled ; not really; need to fix this
  test-javac
  #_(delete-file-recursively (:compile-path dev-deps-project) true)
  #_(javac dev-deps-project)
  (is (.exists (file "test_projects/dev-deps-only/classes"
                     "dev_deps_only" "Junk.class")))
  (is (.exists (file "test_projects/dev-deps-only/classes"
                     "dev_deps_only" "Junk2.class"))))
