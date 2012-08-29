(ns leiningen.core.test.eval
  (:use [clojure.test]
        [leiningen.core.eval])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [leiningen.core.classpath :as classpath])
  (:import (java.io File)))

(def project {:dependencies '[[org.clojure/clojure "1.3.0"]]
              :root "/tmp/lein-sample-project"
              :target-path "/tmp/lein-sample-project/target"
              :source-path ["/tmp/lein-sample-project/src"]
              :resources-path ["/tmp/lein-sample-project/resources"]
              :test-path ["/tmp/lein-sample-project/test"]
              :compile-path "/tmp/lein-sample-project/classes"})

(deftest test-eval-in-project
  (doseq [where [:subprocess :leiningen :classloader]]
    (let [file (File/createTempFile "lein-eval-test" nil)]
      (eval-in-project (assoc project :eval-in where
                              :prep-tasks [])
                       `(spit ~(.getPath file) "foo"))
      (is (= "foo" (slurp file)))
      (.delete file))))

(deftest test-jvm-opts
  (is (= ["-Dhello=\"guten tag\"" "-XX:+HeapDumpOnOutOfMemoryError"]
         (get-jvm-opts-from-env (str "-Dhello=\"guten tag\" "
                                     "-XX:+HeapDumpOnOutOfMemoryError")))))

(deftest test-file-encoding-in-jvm-args
  (is (contains? 
          (set (#'leiningen.core.eval/get-jvm-args project))
          (str "-Dfile.encoding=" (System/getProperty "file.encoding")))))

(deftest test-get-jvm-args-with-proxy-settings
  ;; Mock get-proxy-settings to return test values
  (with-redefs [classpath/get-proxy-settings
                (constantly {:host "foo.com" :port "8080"})]
    (let [args (set (shell-command project 'repl))]
      (is (and (contains? args "-Dhttp.proxyHost=foo.com")
               (contains? args "-Dhttp.proxyPort=8080"))))))
