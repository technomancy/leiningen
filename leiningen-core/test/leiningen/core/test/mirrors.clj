(ns leiningen.core.test.mirrors
  (:require [clojure.test :refer :all]
            [cemerick.pomegranate :as pom]
            [leiningen.core.project :refer [init-project]]))

;; Regression test for issue #1555
(deftest ^:online mirrors-work-ok-with-plugins
  ;; turn off add-classpath so we don't actually mutate the classpath;
  ;; we're still hitting the internet, and there's probably something
  ;; in pomegranate we could redef to prevent that, but I haven't
  ;; figured out what it is exactly.
  (with-redefs [pom/add-classpath (constantly nil)]
    (is (init-project
         ;; we need to use a sequence of pairs rather than a map to
         ;; reproduce the bug; IRL maps got converted to seqs somehow or
         ;; another anyhow, but apparently not by init-project.
         {:mirrors [["central" {:name "foo"
                                :url "http://repo1.maven.org/maven2/"}]]
          ;; Have to have a plugin to reproduce
          :plugins '[[lein-pprint "1.1.1"]]}))))
