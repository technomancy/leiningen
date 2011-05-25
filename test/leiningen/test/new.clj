(ns leiningen.test.new
  (:require [leiningen.new] :reload)
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.util.file :only [delete-file-recursively]]))

(deftest test-new
  (leiningen.new/new "a.b/test-new-proj")
  (is (.exists (file "test-new-proj" "src" "a" "b" "test_new_proj" "core.clj")))
  (delete-file-recursively (file "test-new-proj") false))
