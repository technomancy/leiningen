(ns lein-test-exit-code.core-test
  (:require [clojure.test :refer [deftest is]]))

(defmacro gen-failing-deftests [n]
  `(do
     ~@(for [i (range n)]
         `(deftest ~(symbol (str "expected-failure-" i))
            (is false "Expected failure.")))))

(gen-failing-deftests 256)
