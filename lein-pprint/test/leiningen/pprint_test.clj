(ns leiningen.pprint-test
  (:require [clojure.test :refer :all]
            [leiningen.pprint :refer :all]))

(def shallow {:foo "str"})
(def deep {:foo {:bar "str"}})

(deftest can-pprint-shallow
  (is (= "\"str\"\n" (with-out-str (pprint shallow ":foo")))))

(deftest can-pprint-deep

  (is (= "\"str\"\n" (with-out-str (pprint deep "[:foo :bar]")))))

(deftest can-println-shallow
  (is (= "str\n" (with-out-str (pprint shallow "--no-pretty" "--" ":foo")))))

(deftest can-println-deep
  (is (= "str\n" (with-out-str (pprint deep "--no-pretty" "--" "[:foo :bar]")))))
