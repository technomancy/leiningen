(ns test-leiningen
  (:use [leiningen.main] :reload-all)
  (:use [clojure.test]))

(deftest test-deps
  (-main "deps")
  (is (not (empty? (.listFiles (java.io.File.
                                (str
                                 ;; (:root leiningen/nomnomnom)
                                 "../lib/")))))))
