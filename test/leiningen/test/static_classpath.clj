(ns leiningen.test.static-classpath
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [leiningen.static-classpath :as sc]
            [leiningen.test.helper :as helper])
  (:import (java.io File)))

(defn- relativize [paths-str root]
  (for [path (str/split paths-str #":")]
    (-> path
        (str/replace root "")
        (str/replace (System/getenv "HOME") "~"))))

(deftest test-static-classpath
  (let [tmp (File/createTempFile "lein" "static-cp")
        {:keys [root]} helper/with-resources-project]
    (try
      (sc/static-classpath {:root root} (str tmp))
      (is (= #{"/test" "/src" "/resources" "/%s/classes"
               "~/.m2/repository/org/clojure/clojure/1.3.0/clojure-1.3.0.jar"
               "~/.m2/repository/janino/janino/2.5.15/janino-2.5.15.jar"}
             (set (relativize (slurp tmp) root))))
      (finally
        (.delete tmp)))))
