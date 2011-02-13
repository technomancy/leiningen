(ns test-nom-nom-nom
  (:use [nom.nom.nom]
        [clojure.test]))

(defn f [x]
  (.list x))

(deftest should-use-1.1.0
  (is (= "1.1.0" (clojure-version)))
  (f (java.io.File. "/tmp")))
