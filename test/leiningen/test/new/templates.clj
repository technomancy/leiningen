(ns leiningen.test.new.templates
  (:use clojure.test
        leiningen.new.templates))

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
  (is (= (name-to-path "foo-bar.baz") "foo_bar/baz")))

