(prn "loading" 'lein-test-reload-bug.a-deftype)

(ns lein-test-reload-bug.a-deftype
  (:require [lein-test-reload-bug.b-protocol
             :refer [B]]))

(deftype A []
  B
  (b [this]))
