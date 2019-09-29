(ns leiningen.test.new.templates
  (:use clojure.test
        leiningen.new.templates)
  (:require [leiningen.test.helper :refer [abort-msg] :as lthelper]
            [leiningen.core.user :as user]
            [clojure.java.io :as io])
  (:import [java.io File]))

(defn- getenv [s]
  (System/getenv s))

(deftest line-separators
  (testing "that nothing changes when we're on unix systems"
    (with-redefs [user/getprop (constantly "\n")]
      (is (= (fix-line-separators "foo") "foo"))
      (is (= (fix-line-separators "bar\nbaz") "bar\nbaz"))
      (is (= (fix-line-separators "quux\n\n\nsycorax") "quux\n\n\nsycorax"))))

  (testing "that newlines are correctly converted on '\\r\\n' systems"
    (with-redefs [user/getprop (constantly "\r\n")]
      (is (= (fix-line-separators "foo") "foo"))
      (is (= (fix-line-separators "bar\nbaz") "bar\r\nbaz"))
      (is (= (fix-line-separators "quux\n\n\nsycorax")
             "quux\r\n\r\n\r\nsycorax"))))

  (testing "that other bizarre systems get same treatment"
    (with-redefs [user/getprop (constantly "\t\t")]
      (is (= (fix-line-separators "foo") "foo"))
      (is (= (fix-line-separators "bar\nbaz") "bar\t\tbaz"))
      (is (= (fix-line-separators "quux\n\n\nsycorax")
             "quux\t\t\t\t\t\tsycorax"))))

  (testing "that one can override the normal system newline"
    (with-redefs [user/getprop (constantly "\r\n")
                  user/getenv (fn [s] (if (= s "LEIN_NEW_UNIX_NEWLINES")
                                        "y"
                                        (getenv s)))]
      (is (= (fix-line-separators "foo") "foo"))
      (is (= (fix-line-separators "bar\nbaz") "bar\nbaz"))
      (is (= (fix-line-separators "quux\n\n\nsycorax") "quux\n\n\nsycorax")))))

(deftest project-names
  (is (= (project-name "org.example/foo.bar") "foo.bar"))
  (is (= (project-name "example") "example"))
  (is (= (sanitize-ns "org.example/foo-bar") "org.example.foo-bar"))
  (is (= (sanitize-ns "foo-bar") "foo-bar"))
  (is (= (sanitize-ns "foo_bar") "foo-bar")))

(deftest namespaces
  (is (= (multi-segment "foo") "foo.core"))
  (is (= (multi-segment "foo" "api") "foo.api"))
  (is (= (multi-segment "multi.segment" "last") "multi.segment")))

(deftest paths
  (is (= (name-to-path "foo-bar.baz") (lthelper/fix-path-delimiters "foo_bar/baz"))))

(deftest renderers
  (is (.contains (abort-msg (renderer "my-template") "boom" {})
                 "Template resource 'leiningen/new/my_template/boom' not found."))
  (is (.contains (abort-msg (renderer "my-template") "boom")
                 "Template resource 'leiningen/new/my_template/boom' not found.")))

(deftest slurp-resource-compatibility ; can be removed in 3.0.0
  (is (= (slurp-resource "leiningen/new/template/temp.clj")
         (slurp-resource (io/resource "leiningen/new/template/temp.clj")))))

(deftest files
  (testing "that files marked as executable are set executable"
    (let [file (File/createTempFile "lein" "template")
          path [(.getName file) (.getAbsolutePath file) :executable true]]
      (binding [*dir* (.getParentFile file)
                *force?* true]
        (.deleteOnExit file)
        (->files {} path)
        (is (.canExecute file))))))
