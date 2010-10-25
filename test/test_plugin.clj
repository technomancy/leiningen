(ns test-plugin
  (:use [leiningen.plugin] :reload)
  (:use [clojure.test]))

(deftest test-plugin-standalone-filename
  (is (= (plugin-standalone-filename "tehgroup" "tehname" "0.0.1")
         "tehgroup-tehname-0.0.1.jar"))
  (is (= (plugin-standalone-filename nil "tehname" "0.0.1")
         "tehname-0.0.1.jar")))

(deftest test-extract-name-and-group
  (is (= (extract-name-and-group "tehgroup/tehname")
         ["tehname" "tehgroup"]))
  (is (= (extract-name-and-group "tehname")
         ["tehname" nil])))

;; TODO: figure out a clever way to actually test instaling
;; (deftest test-install
;; (install "lein-plugin" "0.1.0")
;; )