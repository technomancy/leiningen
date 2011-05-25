(ns leiningen.retest
  "Run only the test namespaces which failed last time around."
  (:require [leiningen.test]))

(defn retest
  "Run only the test namespaces which failed last time around.
Requires loading leiningen.hooks.retest to :hooks in project.clj."
  [project & selectors]
  ;; TODO: detect branch change; clear failure list
  (apply leiningen.test/test project
         (concat (if (.exists (java.io.File. ".lein-failures"))
                   (->> (slurp ".lein-failures")
                        read-string sort (map name)))
                 selectors)))
