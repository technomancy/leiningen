(ns leiningen.core.test.eval
  (:require [clojure.test :refer :all]
            [leiningen.core.eval :refer :all]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [leiningen.core.classpath :as classpath]
            [leiningen.test.helper :as lthelper]
            [leiningen.core.project :as project])
  (:import (java.io File)))

(def project {:managed-dependencies '[[org.clojure/clojure "1.3.0"]]
              :dependencies '[[org.clojure/clojure]]
              :root "/tmp/lein-sample-project"
              :repositories project/default-repositories
              :target-path "/tmp/lein-sample-project/target"
              :source-paths ["/tmp/lein-sample-project/src"]
              :resource-paths ["/tmp/lein-sample-project/resources"]
              :test-paths ["/tmp/lein-sample-project/test"]
              :compile-path "/tmp/lein-sample-project/classes"
              :name "test" :group "test" :version "1.0.0"})

(deftest test-eval-in-project
  (doseq [where [:subprocess :leiningen :classloader]]
    (let [file (File/createTempFile "lein-eval-test" "")]
      (eval-in-project (assoc project :eval-in where
                              :prep-tasks [])
                       `(spit ~(.getPath file) (eval "{:foo \"bar\"}")))
      (is (= "{:foo \"bar\"}" (slurp file)))
      (.delete file))))

(deftest test-classpath-directories-created
  (doseq [path (concat (:source-paths project)
                       (:test-paths project)
                       (:resource-paths project))]
    (let [file (File/createTempFile "lein-eval-test" "")]
      (eval-in-project project
                       `(do (.mkdirs (clojure.java.io/file ~path))
                            (spit ~(str path "/foo.txt") "Hello World")
                            (when-let [f# (clojure.java.io/resource "foo.txt")]
                              (spit ~(.getPath file) (slurp f#))))
                       `(require 'clojure.java.io))
      (is (= "Hello World" (slurp file)))
      (.delete (io/file (str path "/foo.txt")))
      (.delete (io/file path))
      (.delete file))))

(deftest test-jvm-opts
  (is (= ["-Dhello=\"guten tag\"" "-XX:+HeapDumpOnOutOfMemoryError"]
         (get-jvm-opts-from-env (str "-Dhello=\"guten tag\" "
                                     "-XX:+HeapDumpOnOutOfMemoryError"))))
  (is (= ["-Dfoo=bar" "-Dbar=baz"]
         (get-jvm-opts-from-env (str "    -Dfoo=bar"
                                     "    -Dbar=baz"))))
  (is (= ["-Dfoo='ba\"r'" "-Dbar=\"ba\"'z'" "arg"]
         (get-jvm-opts-from-env (str "    -Dfoo='ba\"r'"
                                     "    -Dbar=\"ba\"'z'"
                                     "    arg"))))
  (is (nil? (parse-d-property "-Xmx1g")))
  (is (= ["line.separator" "\n"]
         (parse-d-property "-Dline.separator=\n"))))

(deftest test-file-encoding-in-jvm-args
  (is (contains?
          (set (#'leiningen.core.eval/get-jvm-args project))
          (str "-Dfile.encoding=" (System/getProperty "file.encoding")))))

(deftest test-get-jvm-args-with-proxy-settings
  ;; Mock get-proxy-settings to return test values
  (with-redefs [classpath/get-proxy-settings
                (fn ([] {:host "foo.com" :port 8080})
                    ([https] {:host "secure-foo.com", :port 443}))]
    (let [args (set (shell-command project 'repl))]
      (is (and (contains? args "-Dhttp.proxyHost=foo.com")
               (contains? args "-Dhttp.proxyPort=8080")
               (contains? args "-Dhttps.proxyHost=secure-foo.com")
               (contains? args "-Dhttps.proxyPort=443"))))))

(deftest test-java-agent
  (let [p {:java-agents '[[com.newrelic.agent.java/newrelic-agent "2.18.0"
                           :bootclasspath true]
                          [nodisassemble "0.1.2" :options "hello"]]
           :dependencies '[[slamhound "1.3.0"]]
           :repositories project/default-repositories}
        [newrelic newrelic-bootcp nodisassemble] (classpath-arg p)]
    (is (.endsWith newrelic (lthelper/fix-path-delimiters
                              (str "/com/newrelic/agent/java/newrelic-agent"
                                   "/2.18.0/newrelic-agent-2.18.0.jar"))))
    (is (re-find #"bootclasspath.*newrelic.*jar" newrelic-bootcp))
    (is (re-find #"-javaagent:.*nodisassemble-0.1.2.jar=hello" nodisassemble))))

(deftest test-sh-with-exit-code-successful-command
  (with-redefs [sh (constantly 0)]
    (is (= 0 (sh-with-exit-code "Shouldn't see me." "ls")))))

(deftest test-sh-with-exit-code-failed-command
  (with-redefs [sh (constantly 1)]
    (is (thrown-with-msg? Exception #"Should see me. ls exit code: 1" (sh-with-exit-code "Should see me" "ls")))))
