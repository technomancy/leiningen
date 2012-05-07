(ns leiningen.test.search
  (:require [clojure.java.io :as io])
  (:use [clojure.test]
        [leiningen.search]))

(deftest test-searchy
  (with-redefs [remote-index-url (constantly
                                  (io/resource "sample-index.zip"))]
    (ensure-fresh-index ["test" {:url "http://example.com/repo"}])
    (is (= #{"segments.gen" "_0.cfx" "timestamp" "_0.cfs" "segments_2"}
           (set (.list (index-location "http://example.com/repo")))))
    (let [results (search-repository ["test" {:url "http://example.com/repo"}]
                                     "hooke" 1)]
      (is (= '#{[[robert/hooke "\"1.0.0\""] "Hooke your functions!"]
                [[robert/hooke "\"1.0.1\""] "Hooke your functions!"]
                [[robert/hooke "\"1.0.2\""] "Hooke your functions!"]
                [[robert/hooke "\"1.1.0\""] "Hooke your functions!"]}
             (set (map parse-result results)))))))
