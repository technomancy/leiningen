(ns leiningen.test.pprint
  (:require [clojure.test :refer :all]
            [leiningen.pprint :refer :all]))

(def shallow {:foo "str"})
(def deep {:foo {:bar "str"}})

(deftest test-pprint-key
  (is (= "\"str\"\n" (with-out-str (pprint shallow ":foo")))))

(deftest test-pprint-seq
  (is (= "\"str\"\n" (with-out-str (pprint deep "[:foo :bar]")))))

(deftest test-pprint-key-no-pretty
  (is (= "str\n" (with-out-str (pprint shallow "--no-pretty" "--" ":foo")))))

(deftest test-pprint-seq-no-pretty
  (is (= "str\n" (with-out-str (pprint deep "--no-pretty" "--" "[:foo :bar]")))))

(deftest test-pprint-project
  (is (= "{:foo \"str\"}\n" (with-out-str (pprint shallow)))))

(deftest test-pprint-project-no-pretty
  (is (= "{:foo str}\n" (with-out-str (pprint shallow "--no-pretty" "--")))))
