(ns leiningen.retest
  "Run only the test namespaces which failed last time around."
  (:require [leiningen.test]))

(defn retest
  "Run only the test namespaces which failed last time around.
Requires loading leiningen.hooks.retest to :hooks in project.clj."
  [project]
  ;; TODO: detect branch change; clear failure list
  (if (.exists (java.io.File. ".lein-failures"))
    (apply leiningen.test/test project (->> (slurp ".lein-failures")
                                            read-string sort (map name)))
    (leiningen.test/test project)))
