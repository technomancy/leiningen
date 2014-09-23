(ns leiningen.test.jvm-opts
  (:require [leiningen.with-profile :refer [with-profile]]
            [leiningen.test.helper :refer [jvm-opts-project]])
  (:use clojure.test))

;; This is a regression test for technomancy/leiningen#1676 (make sure
;; that file.encoding can be overriden by profiles.)
(deftest file-encoding-conveyed
  (let [exec '(println "system encoding" (System/getProperty "file.encoding"))
        run-with #(with-out-str
                    (with-profile jvm-opts-project % "run"
                      "-m" "clojure.main"
                      "-e" (pr-str exec)))]
    (testing "baseline sane"
      (is (.contains (run-with "+no-op") "system encoding UTF-8")))
    (testing "accepts alternative"
      (is (.contains (run-with "+ascii") "system encoding ASCII")))))
