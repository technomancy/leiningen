(ns lein-test-exit-code.core-test
  (:require [clojure.test :refer [deftest is]]))

(defmacro gen-failing-deftests [n]
  `(do 
     ~@(for [i (range n)]
         `(deftest ~(symbol (str "failing-" i))
            (is false)))))

(gen-failing-deftests 256)
