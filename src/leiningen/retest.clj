(ns leiningen.retest
  "Run only the test namespaces which failed last time around."
  (:require [leiningen.test :as test]
            [leiningen.core.main :as main]))

(defn retest
  "Run only the test namespaces which failed last time around."
  [project & selectors]
  (if (:monkeypatch-clojure-test project true)
    (apply test/test project
           (concat (if (.exists (java.io.File. ".lein-failures"))
                     (->> (slurp ".lein-failures")
                          read-string sort (map name)))
                   selectors))
    (main/abort "Cannot retest when :monkeypatch-clojure-test is disabled.")))
