(ns leiningen.core.test.mirrors
  (:require [clojure.test :refer :all]
            [cemerick.pomegranate :as pom]
            [leiningen.core.project :refer [defproject init-project]]))


;; Regression test for Issue #1555


(defproject mirrors-work-ok-with-plugins-project "0.0.0"
  ;; we need to use a sequence of pairs rather than a map to
  ;; reproduce the bug; IRL maps got converted to seqs somehow or
  ;; another anyhow, but apparently not by init-project.
  :mirrors [["central" {:name "foo"
                        :url "https://repo1.maven.org/maven2/"}]]
  ;; Have to have a plugin to reproduce
  :plugins [[lein-pprint "1.1.1"]])

(deftest ^:online mirrors-work-ok-with-plugins
  ;; turn off add-classpath so we don't actually mutate the classpath;
  ;; we're still hitting the internet, and there's probably something
  ;; in pomegranate we could redef to prevent that, but I haven't
  ;; figured out what it is exactly.
  (with-redefs [pom/add-classpath (constantly nil)]
    (is (init-project project))))
