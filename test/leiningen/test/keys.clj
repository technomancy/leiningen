(ns leiningen.test.keys
  (:require [clojure.test :refer :all]))

(deftest technomancy-gpg-key
  (let [month-before-key-expiry #inst "2014-06-16"]
    (is (< (System/currentTimeMillis) (.getTime month-before-key-expiry))
        "If this fails, yell at technomancy to generate a new key!")))

(deftest clojars-ssl-cert
  (let [month-before-cert-expiry #inst "2014-10-09"]
    (is (< (System/currentTimeMillis) (.getTime month-before-cert-expiry))
        "If this fails, yell at _ato to get a new SSL cert.")))
