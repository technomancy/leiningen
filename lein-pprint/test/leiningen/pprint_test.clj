(ns leiningen.pprint-test
  (:require [clojure.test :refer :all]
            [leiningen.pprint :refer :all]))

(deftest can-pprint
  (is (= "1\n" (with-out-str (pprint {:foo 1} ":foo"))))
  (is (= "1\n" (with-out-str (pprint {:foo {:bar 1}} "[:foo :bar]")))))
