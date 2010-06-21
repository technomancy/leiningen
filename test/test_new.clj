(ns test-new
  (:use [clojure.test]
        [clojure.contrib.io :only [delete-file-recursively file]]
        [clojure.contrib.shell :only [with-sh-dir sh]]))

(deftest test-new
  (sh (if (= "Windows" (System/getProperty "os.name"))
        "lein.bat" "lein") "new" "a.b/test-new-proj")
  (is (.exists (file "test-new-proj" "src" "a" "b" "test_new_proj" "core.clj")))
  (delete-file-recursively (file "test-new-proj") false))
