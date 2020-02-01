(ns leiningen.pprint-test
  (:require [clojure.test :refer :all]
            [leiningen.pprint :refer :all]))

(def shallow {:foo "str"})
(def deep {:foo {:bar "str"}})

(deftest can-pprint-key
  (is (= "\"str\"\n" (with-out-str (pprint shallow ":foo")))))

(deftest can-pprint-seq
  (is (= "\"str\"\n" (with-out-str (pprint deep "[:foo :bar]")))))

(deftest can-pprint-key-no-pretty
  (is (= "str\n" (with-out-str (pprint shallow "--no-pretty" "--" ":foo")))))

(deftest can-pprint-seq-no-pretty
  (is (= "str\n" (with-out-str (pprint deep "--no-pretty" "--" "[:foo :bar]")))))

(deftest can-pprint-project
  (is (= "{:foo \"str\"}\n" (with-out-str (pprint shallow)))))

(deftest can-pprint-project-no-pretty
  (is (= "{:foo str}\n" (with-out-str (pprint shallow "--no-pretty" "--")))))
