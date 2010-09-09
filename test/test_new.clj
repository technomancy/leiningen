(ns test-new
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.util.file :only [delete-file-recursively]]
        [clojure.java.shell :only [with-sh-dir sh]]))

(deftest test-new
  (sh (if (= "Windows" (System/getProperty "os.name"))
        "lein.bat" "lein") "new" "a.b/test-new-proj")
  (is (.exists (file "test-new-proj" "src" "a" "b" "test_new_proj" "core.clj")))
  (delete-file-recursively (file "test-new-proj") false))
