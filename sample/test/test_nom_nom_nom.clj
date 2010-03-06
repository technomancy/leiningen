(ns test-nom-nom-nom
  (:use [nom.nom.nom]
        [clojure.test]))

(deftest should-use-1.1.0-SNAPSHOT
  (is (= "1.1.0-master-SNAPSHOT" (clojure-version))))
