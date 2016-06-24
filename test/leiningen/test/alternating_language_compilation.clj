;; SPOILER ALERT: This file contains the answer to Project Euler #65,
;; Convergents of e (https://projecteuler.net/problem=65). If you intend
;; to solve this problem on your own, don't look too deeply into expected-output.
(ns leiningen.test.alternating-language-compilation
  "Tests compilation of Java that depends on Clojure that depends on Java..."
  (:require [clojure.string :as string]
            [leiningen.clean :refer [clean]]
            [leiningen.run :refer [run]]
            [leiningen.test.helper
             :refer [alternating-language-compilation-project
                     with-system-out-str]])
  (:import java.io.File)
  (:use clojure.test))

(def ^:private expected-output
  (string/join (System/getProperty "line.separator")
               [(str "The 100th expansion of e is "
                     "6963524437876961749120273824619538346438023188214475670667"
                     "/2561737478789858711161539537921323010415623148113041714756")
                "The sum of the digits of the numerator is 272"
                ""]))

(def ^:private target-proj alternating-language-compilation-project)

(deftest test-alternating-language-compilation
  (testing "Compilation can alternate between Java and Clojure and Java and..."
    (do (clean target-proj)
        (let [output-from-run (with-system-out-str (run target-proj))]
          (is (= output-from-run expected-output))))))
