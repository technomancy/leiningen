(ns leiningen.test.check
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [leiningen.check :as check]
            [leiningen.clean :as clean]
            [leiningen.core.main :as main]
            [leiningen.test.helper :as h])
  (:import (java.io ByteArrayOutputStream PrintStream)))

(deftest works-with-aot
  (binding [main/*exit-process?* false]
    (let [project (doto (h/read-test-project "reflector") clean/clean)
          old-err System/err
          out (ByteArrayOutputStream.)]
      (System/setErr (PrintStream. out))
      (try (binding [*err* (io/writer out)]
             (check/check project))
           (catch clojure.lang.ExceptionInfo e
             (when-not (is (= 1 (:exit-code (ex-data e))))
               (throw e)))
           (finally (System/setErr old-err)))
      (let [out-str (.toString out "UTF-8")]
        (is (re-find #"field getBytes can't be resolved" out-str))
        (is (not (re-find #"ClassNotFoundException" out-str)))))))
