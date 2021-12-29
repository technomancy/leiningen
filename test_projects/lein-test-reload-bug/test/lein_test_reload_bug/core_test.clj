(ns lein-test-reload-bug.core-test
  (:require [clojure.test :refer [deftest is]]
            [lein-test-reload-bug.a-deftype :refer [->A]]
            [lein-test-reload-bug.b-protocol :refer [b]]))

(deftest a-test
  (let [a (->A)]
    (is (= :ok (b a)))))
