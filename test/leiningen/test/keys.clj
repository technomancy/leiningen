(ns leiningen.test.keys
  (:require [clojure.test :refer :all]))

(deftest technomancy-gpg-key
  (let [month-before-key-expiry (java.util.Date. 114 5 16)] ; 16 june 2014
    (is (< (System/currentTimeMillis) (.getTime month-before-key-expiry))
        "If this fails, yell at technomancy to generate a new key!")))

(deftest clojars-ssl-cert
  (let [month-before-cert-expiry (java.util.Date. 114 9 9)] ; 9 nov 2014
    (is (< (System/currentTimeMillis) (.getTime month-before-cert-expiry))
        "If this fails, yell at _ato to get a new SSL cert.")))
