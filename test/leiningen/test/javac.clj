(ns leiningen.test.javac
  (:require [clojure.string :as string])
  (:import [java.io BufferedReader StringReader])
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.javac :only [javac
                                normalize-javac-options
                                uncommented-lines-from-reader
                                extract-package-from-line-seq]]
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

(defn- inject-newlines [& args]
  (string/join (System/getProperty "line.separator") args))

(def ^:private comment-stripping-data
  [[(inject-newlines
     "This isn't Java,"
     "But it does share comment semantics.")
    (inject-newlines
     "This isn't Java,"
     "But it does share comment semantics.")]
   [(inject-newlines
     "See? Here's a // trailing comment,"
     "A line after it,"
     "And a // trailing comment again.")
    (inject-newlines
     "See? Here's a "
     "A line after it,"
     "And a ")]
   [(inject-newlines
     "/* But block comments */"
     "also work.")
    (inject-newlines
     ""
     "also work.")]
   [(inject-newlines
     "/* Even"
     "across"
     "line */"
     "delimiters")
    (inject-newlines
     ""
     ""
     ""
     "delimiters")]
   [(inject-newlines
     "Even /* when started"
     "in the middle */"
     "of a line")
    (inject-newlines
     "Even "
     ""
     "of a line")]
   [(inject-newlines
     "Even /* when stopping"
     "in the */ middle of"
     "a line too")
    (inject-newlines
     "Even "
     " middle of"
     "a line too")]
   [(inject-newlines
     "However, a // line comment /*"
     "supersedes a /* block comment */")
    (inject-newlines
     "However, a "
     "supersedes a ")]
   [(inject-newlines
     "But, /* you */ can /*"
     "string */ a /* few */ of /*"
     "these */ along // together")
    (inject-newlines
     "But,  can "
     " a  of "
     " along ")]])

(deftest test-javac-comment-stripping
  (testing "comments are stripped in lexed-lines-from-reader"
    (loop [test-cases comment-stripping-data]
      (if (empty? test-cases)
        true ;; we passed
        (let [[given expected] (first test-cases)
              given-as-reader  (-> given (StringReader.) (BufferedReader.))
              given-as-seq     (uncommented-lines-from-reader given-as-reader)
              lexed-as-str     (apply inject-newlines given-as-seq)]
          (if (is (= lexed-as-str expected))
            (recur (rest test-cases))
            false)))))) ;; we failed

(def ^:private package-extraction-data
  [[(inject-newlines
     "import java.util.UUID;"
     "import java.util.Set;"
     ""
     "public class Main {"
     "    public static void main(String[] args) {"
     "        System.out.println(\"Hello, World!\");"
     "    }"
     "}")
    nil]
   [(inject-newlines
     "package my.java.Main;"
     ""
     "public class Main {"
     "    public static void main(String[] args) {"
     "        System.out.println(\"Hello, World!\");"
     "    }"
     "}")
    "my.java.Main"]
   [(inject-newlines
     ""
     "    "
     "package my.java.Main;      "
     "public class Main {"
     "    public static void main(String[] args) {"
     "        System.out.println(\"Hello, World!\");"
     "    }"
     "}")
    "my.java.Main"]
   [(inject-newlines
     "package"
     "my.java.Main"
     ";"
     "public class Main {"
     "    public static void main(String[] args) {"
     "        System.out.println(\"Hello, World!\");"
     "    }"
     "}")
    "my.java.Main"]
   [(inject-newlines
     "package      my.java.Main;;;;;"
     "public class Main {"
     "    public static void main(String[] args) {"
     "        System.out.println(\"Hello, World!\");"
     "    }"
     "}")
    "my.java.Main"]
   [(inject-newlines
     "   package   my.java.Main;  "
     "public class Main {"
     "    public static void main(String[] args) {"
     "        System.out.println(\"Hello, World!\");"
     "    }"
     "}")
    "my.java.Main"]
   [(inject-newlines
     "package"
     ""
     "")
    nil]])

(deftest test-package-extraction
  (loop [test-data-queue  package-extraction-data
         test-data-offset 0]
    (if (empty? test-data-queue)
      true ;; assume we succeeded
      (let [[test-data expected] (first test-data-queue)
            test-data-seq        (-> test-data
                                     (StringReader.)
                                     (BufferedReader.)
                                     (uncommented-lines-from-reader))]
        (if-not (is (= (extract-package-from-line-seq test-data-seq) expected))
          (binding [*out* *err*]
            (println "test-package-extraction failure at offset"
                     test-data-offset)
            false)
          (recur (rest test-data-queue) (inc test-data-offset)))))))
