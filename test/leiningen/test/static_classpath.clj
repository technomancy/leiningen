(ns leiningen.test.static-classpath
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [leiningen.core.main :as main]
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
      ;; static protections will wipe out these things; with-redefs can restore
      ;; them back to their root values for us
      (with-redefs [*read-eval* false
                    eval eval
                    load-file load-file]
        (try (binding [main/*cwd* root]
               (main/-main "static-classpath" (str tmp)))
             (catch Exception e
               ;; suppressed exit
               (is (zero? (:exit-code (ex-data e)))))))
      (is (= #{"/test" "/src" "/resources" "/%s/classes"
               "~/.m2/repository/org/clojure/clojure/1.3.0/clojure-1.3.0.jar"
               "~/.m2/repository/janino/janino/2.5.15/janino-2.5.15.jar"}
             (set (relativize (slurp tmp) root))))
      (finally
        (.delete tmp)))))
