(ns test-new
  (:use [clojure.test]
	[clojure.contrib.java-utils :only [delete-file-recursively file]]
	[clojure.contrib.shell-out :only [with-sh-dir sh]]))

(deftest test-new
  (sh "lein" "new" "a.b/test-new-proj")
  (is (.exists (file "test-new-proj" "src" "a" "b" "test_new_proj.clj")))
  (delete-file-recursively (file "test-new-proj") false))