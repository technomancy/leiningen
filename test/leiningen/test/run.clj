(ns leiningen.test.run
  (:require [leiningen.core.project :as project])
  (:use [clojure.test]
        [clojure.java.io :only [delete-file]]
        ;; [leiningen.javac :only [javac]]
        [leiningen.run]
        [leiningen.util.file :only [tmp-dir]]
        [leiningen.test.helper :only [tricky-name-project dev-deps-project]]))

(def out-file (format "%s/lein-test" tmp-dir))

(use-fixtures :each (fn [f]
                      (f)
                      (delete-file out-file :silently)))

(deftest test-basic
  (is (zero? (run tricky-name-project "/unreadable")))
  (is (= "nom:/unreadable" (slurp out-file))))

(deftest test-alt-main
  (is (zero? (run tricky-name-project "-m" "org.domain.tricky-name.munch"
                  "/unreadable")))
  (is (= ":munched (\"/unreadable\")" (slurp out-file))))

(deftest test-aliases
    (is (zero? (run tricky-name-project ":bbb" "/unreadable")))
    (is (= "BRUNCH" (slurp out-file)))
    (delete-file out-file :silently)
    (is (zero? (run tricky-name-project ":mmm" "/unreadable")))
    (is (= ":munched (\"/unreadable\")" (slurp out-file))))

(deftest test-escape-args
  (is (zero? (run tricky-name-project "--" ":bbb")))
  (is (= "nom::bbb" (slurp out-file)))
  (is (zero? (run tricky-name-project "--" "-m")))
  (is (= "nom:-m" (slurp out-file))))

;; TODO: re-enable
;; (deftest test-run-java-main
;;   (javac dev-deps-project)
;;   (is (zero? (run dev-deps-project))))
