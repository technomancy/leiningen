(ns leiningen.test.run
  (:use [clojure.test]
        [clojure.java.io :only [delete-file]]
        [leiningen.core :only [read-project]]
        [leiningen.run]
        [leiningen.util.file :only [tmp-dir]]))

(def out-file (format "%s/lein-test" tmp-dir))

(def project (binding [*ns* (find-ns 'leiningen.core)]
               (read-project "test_projects/tricky-name/project.clj")))

(use-fixtures :each (fn [f]
                      (f)
                      (delete-file out-file :silently)))

(deftest test-basic
  (is (zero? (run project "/unreadable")))
  (is (= "nom:/unreadable" (slurp out-file))))

(deftest test-alt-main
  (is (zero? (run project "-m" "org.domain.tricky-name.munch" "/unreadable")))
  (is (= ":munched (\"/unreadable\")" (slurp out-file))))

(deftest test-aliases
    (is (zero? (run project ":bbb" "/unreadable")))
    (is (= "BRUNCH" (slurp out-file)))
    (delete-file out-file :silently)
    (is (zero? (run project ":mmm" "/unreadable")))
    (is (= ":munched (\"/unreadable\")" (slurp out-file))))

(deftest test-escape-args
  (is (zero? (run project "--" ":bbb")))
  (is (= "nom::bbb" (slurp out-file)))
  (is (zero? (run project "--" "-m")))
  (is (= "nom:-m" (slurp out-file))))
