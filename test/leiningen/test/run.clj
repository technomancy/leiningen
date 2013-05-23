(ns leiningen.test.run
  (:require [leiningen.core.project :as project])
  (:use [clojure.test]
        [clojure.java.io :only [delete-file]]
        ;; [leiningen.javac :only [javac]]
        [leiningen.run]
        [leiningen.test.helper :only [bad-require-project
                                      tmp-dir
                                      tricky-name-project]]))

(def out-file (format "%s/lein-test" tmp-dir))

(use-fixtures :each (fn [f]
                      (f)
                      (delete-file out-file :silently)))

(deftest test-basic
  (run tricky-name-project "/unreadable")
  (is (= "nom:/unreadable" (slurp out-file))))

(deftest test-alt-main
  (run tricky-name-project "-m" "org.domain.tricky-name.munch" "/unreadable")
  (is (= ":munched (\"/unreadable\")" (slurp out-file))))

(deftest test-escape-args
  (run tricky-name-project "--" ":bbb")
  (is (= "nom::bbb" (slurp out-file)))
  (run tricky-name-project "--" "-m")
  (is (= "nom:-m" (slurp out-file))))

(deftest test-bad-require-error-msg
  (let [sw (java.io.StringWriter.)]
    (binding [*err* sw]
      (try (run bad-require-project)
           (catch clojure.lang.ExceptionInfo e nil)))
    (let [e-msg (str sw)]
      ;; Don't throw the old ClassNotFoundException
      (is (not (re-find #"ClassNotFoundException: bad-require.core" e-msg)))
      ;; Do show a relevant error message
      (is (re-find #"FileNotFoundException" e-msg))
      (is (re-find #"this/namespace/does/not/exist.clj" e-msg)))))

;; TODO: re-enable
;; (deftest test-run-java-main
;;   (javac dev-deps-project)
;;   (run dev-deps-project))
