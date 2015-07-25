(ns cljc-test
  (:use #?(:clj  [clojure.test]
           :cljs [cljs.test])
        [selectors :only [record-ran]]))

(deftest conditional-test
  (record-ran :cljc-test)
  (is true))
