(ns leiningen.test.keys
  (:require [clojure.test :refer :all]))

(def df (java.text.SimpleDateFormat. "yyyy-MM-dd"))

#_(deftest technomancy-gpg-key
    (let [month-before-key-expiry (.parse df "2017-05-14")]
      (is (< (System/currentTimeMillis) (.getTime month-before-key-expiry))
          "If this fails, yell at technomancy to generate a new key!")))

(deftest clojars-ssl-cert
  (let [month-before-cert-expiry (.parse df "2020-05-17")]
    (is (< (System/currentTimeMillis) (.getTime month-before-cert-expiry))
        "If this fails, yell at tcrawley to update the clojars.pem for use by lein as a client cert.")))
