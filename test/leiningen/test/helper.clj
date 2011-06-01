(ns leiningen.test.helper
  (:require [lancet.core :as lancet])
  (:use [clojure.java.io :only [file]]
        [leiningen.compile :only [platform-nullsink]]
        [leiningen.core :only [read-project]]))

(def local-repo (file (System/getProperty "user.home") ".m2" "repository"))

(defn m2-dir [n v]
  (file local-repo (if (string? n) n (or (namespace n) (name n))) (name n) v))

(defn- read-test-project [name]
  (binding [*ns* (find-ns 'leiningen.core)]
    (read-project (format "test_projects/%s/project.clj" name))))

(def sample-project (read-test-project "sample"))

(def dev-deps-project (read-test-project "dev-deps-only"))

(def sample-failing-project (read-test-project "sample_failing"))

(def sample-no-aot-project (read-test-project "sample_no_aot"))

(def tricky-name-project (read-test-project "tricky-name"))

(def native-project (read-test-project "native"))

(def logger (first (.getBuildListeners lancet/ant-project)))

(defmacro with-no-log [& body]
  `(do (.setOutputPrintStream logger (platform-nullsink))
       (.setErrorPrintStream logger (platform-nullsink))
       (try ~@body
            (finally (.setOutputPrintStream logger System/out)
                     (.setErrorPrintStream logger System/err)))))
