(ns leiningen.core.test.eval
  (:use [clojure.test]
        [leiningen.core.eval])
  (:require [clojure.java.io :as io]
            [clojure.set :as set])
  (:import (java.io File)))

(def project {:dependencies '[[org.clojure/clojure "1.3.0"]]
              :root "/tmp/lein-sample-project"
              :source-path "/tmp/lein-sample-project/src"
              :resources-path "/tmp/lein-sample-project/resources"
              :test-path "/tmp/lein-sample-project/test"
              :compile-path "/tmp/lein-sample-project/classes"})

(deftest test-eval-in-project
  (doseq [where [:subprocess :leiningen :classloader]]
    (let [file (File/createTempFile "lein-eval-test" nil)]
      (eval-in-project
       (assoc project :eval-in where)
       `(spit ~(.getPath file) "foo"))
      (is (= "foo" (slurp file)))
      (.delete file))))
