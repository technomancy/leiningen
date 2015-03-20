(ns leiningen.test.run
  (:require [leiningen.core.project :as project]
            [leiningen.javac]
            [clojure.java.io :as io]
            [leiningen.test.helper :as helper
             :refer [bad-require-project tmp-dir tricky-name-project
                     java-main-project file-not-found-thrower-project
                     with-system-out-str with-system-err-str]])
  (:use [clojure.test]
        [leiningen.run]))

(def out-file (format "%s/lein-test" tmp-dir))

(deftest test-arg-map
  (let [parse-args #'leiningen.run/parse-args]
    (is (= (:main (parse-args ["-m" "my-main"]))
           "my-main"))
    (is (= ((juxt :main :args) (parse-args ["-m" "my-main" "-m" "foo"]))
           ["my-main" ["-m" "foo"]]))
    (is (= (:arg-conversion (parse-args ["-m" "my-main"]))
           :stringify))
    (is (= (:arg-conversion (parse-args ["-m" "my-main" "--quote-args"]))
           :quote))
    (is (= (:arg-conversion (parse-args ["--quote-args" "-m" "my-main"]))
           :quote))
    (is (= (:args (parse-args ["--" "--quote-args" "-m" "my-main"]))
           ["--quote-args" "-m" "my-main"]))
    (is (= (:args (parse-args ["--" "--" "-m" "my-main"]))
           ["--" "-m" "my-main"]))))

(use-fixtures :each (fn [f]
                      (f)
                      (io/delete-file out-file :silently)))

(deftest test-basic
  (run tricky-name-project "/unreadable")
  (is (= "nom:/unreadable" (slurp out-file))))

(deftest test-alt-main
  (run tricky-name-project "-m" "org.domain.tricky-name.munch" "/unreadable")
  (is (= ":munched (\"/unreadable\")" (slurp out-file))))

(deftest test-valid-namespace-argument
  (is (re-find #"Option -m requires a valid namespace argument, not -1\."
               (helper/abort-msg run tricky-name-project "-m" "-1"))))

(deftest test-nonexistant-ns-error-message
  (is (re-find #"Can't find 'nonexistant.ns' as \.class or \.clj for lein run"
               (with-system-err-str
                 (try (run tricky-name-project "-m" "nonexistant.ns")
                      (catch Exception _))))))

(deftest test-escape-args
  (run tricky-name-project "--" ":bbb")
  (is (= "nom::bbb" (slurp out-file)))
  (run tricky-name-project "--" "-m")
  (is (= "nom:-m" (slurp out-file))))

(deftest test-bad-require-error-msg
  (let [e-msg (with-system-err-str
                (try (run bad-require-project)
                     (catch clojure.lang.ExceptionInfo e nil)))]
    ;; Don't throw the old ClassNotFoundException
    (is (not (re-find #"ClassNotFoundException: bad-require.core" e-msg)))
    ;; Do show a relevant error message
    (is (re-find #"FileNotFoundException" e-msg))
    (is (re-find #"this/namespace/does/not/exist.clj" e-msg))))

(deftest test-run-java-main
  (leiningen.javac/javac java-main-project)
  (let [out-result (with-system-out-str (run java-main-project))]
    (is (= (.trim out-result) ;; To avoid os-specific newline handling
            "Hello from Java!"))))

;; Regression test for https://github.com/technomancy/leiningen/issues/1469
(deftest file-not-found-exception-test
  (let [s (with-system-err-str
            (try (run file-not-found-thrower-project
                   "-m" "file-not-found-thrower.core")
                 (catch clojure.lang.ExceptionInfo e nil)))]
    ;; testing that the true exception is printed immediately and
    ;; the inappropriate error message "Can't find
    ;; 'file-not-found-thrower.core' as .class or .clj for lein run:
    ;; please check the spelling." is not
    (is (.startsWith s "Exception in thread \"main\" java.io.FileNotFoundException"))))
