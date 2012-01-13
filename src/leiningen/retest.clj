(ns leiningen.retest
  (:require [leiningen.test :as test]))

(defn retest
  "Run only the test namespaces which failed last time around."
  [project & selectors]
  (apply test/test project
         (concat (if (.exists (java.io.File. ".lein-failures"))
                   (->> (slurp ".lein-failures")
                        read-string sort (map name)))
                 selectors)))
