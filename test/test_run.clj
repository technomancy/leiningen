(ns test-run
  (:use [clojure.test]
        [clojure.java.io :only [delete-file]]
        [leiningen.core :only [read-project]]
        [leiningen.run]
        [leiningen.util.file :only [tmp-dir]]))

(def out-file (format "%s/lein-test" tmp-dir))

(def project (binding [*ns* (find-ns 'leiningen.core)]
               (read-project "test_projects/tricky-name/project.clj")))

(use-fixtures :each (fn [f]
                      (delete-file out-file :silently)
                      (f)))

(deftest test-basic
  (is (zero? (run project "1")))
  (is (= "nom:1" (slurp out-file))))

(deftest test-alt-main
  (is (zero? (run project "-m" "org.domain.tricky-name.munch" "1")))
  (is (= ":munched (\"1\")" (slurp out-file))))

(deftest test-aliases
    (is (zero? (run project ":bbb" "1")))
    (is (= "BRUNCH" (slurp out-file)))
    (delete-file out-file :silently)
    (is (zero? (run project ":mmm" "1")))
    (is (= ":munched (:mmm \"1\")" (slurp out-file))))
