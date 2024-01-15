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
        (str/replace (System/getenv "HOME") "~")
        (str/replace "~/.m2/repository/" "$REPO/"))))

(deftest test-static-classpath
  (let [tmp (File/createTempFile "lein" "static-cp")
        {:keys [root]} helper/leaky-composite-project]
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
      (is (= #{"$REPO/clj-stacktrace/clj-stacktrace/0.2.8/clj-stacktrace-0.2.8.jar"
               "$REPO/commons-codec/commons-codec/1.11/commons-codec-1.11.jar"
               "$REPO/commons-fileupload/commons-fileupload/1.4/commons-fileupload-1.4.jar"
               "$REPO/commons-io/commons-io/2.6/commons-io-2.6.jar"
               "$REPO/crypto-equality/crypto-equality/1.0.0/crypto-equality-1.0.0.jar"
               "$REPO/crypto-random/crypto-random/1.2.0/crypto-random-1.2.0.jar"
               "$REPO/hiccup/hiccup/1.0.5/hiccup-1.0.5.jar"
               "$REPO/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar"
               "$REPO/nrepl/nrepl/1.0.0/nrepl-1.0.0.jar"
               "$REPO/ns-tracker/ns-tracker/0.4.0/ns-tracker-0.4.0.jar"
               "$REPO/org/clojure/clojure/1.10.1/clojure-1.10.1.jar"
               "$REPO/org/clojure/core.specs.alpha/0.2.44/core.specs.alpha-0.2.44.jar"
               "$REPO/org/clojure/java.classpath/0.3.0/java.classpath-0.3.0.jar"
               "$REPO/org/clojure/spec.alpha/0.2.176/spec.alpha-0.2.176.jar"
               "$REPO/org/clojure/tools.namespace/0.2.11/tools.namespace-0.2.11.jar"
               "$REPO/org/eclipse/jetty/jetty-http/9.4.31.v20200723/jetty-http-9.4.31.v20200723.jar"
               "$REPO/org/eclipse/jetty/jetty-io/9.4.31.v20200723/jetty-io-9.4.31.v20200723.jar"
               "$REPO/org/eclipse/jetty/jetty-server/9.4.31.v20200723/jetty-server-9.4.31.v20200723.jar"
               "$REPO/org/eclipse/jetty/jetty-util/9.4.31.v20200723/jetty-util-9.4.31.v20200723.jar"
               "$REPO/org/nrepl/incomplete/0.1.0/incomplete-0.1.0.jar"
               "$REPO/ring/ring-codec/1.1.2/ring-codec-1.1.2.jar"
               "$REPO/ring/ring-core/1.8.2/ring-core-1.8.2.jar"
               "$REPO/ring/ring-devel/1.8.2/ring-devel-1.8.2.jar"
               "$REPO/ring/ring-jetty-adapter/1.8.2/ring-jetty-adapter-1.8.2.jar"
               "$REPO/ring/ring-servlet/1.8.2/ring-servlet-1.8.2.jar"
               "$REPO/ring/ring/1.8.2/ring-1.8.2.jar"
               "/dev-resources"
               "/src"
               "/target/classes"
               "/test"}
             (set (relativize (slurp tmp) root))))
      (finally
        (.delete tmp)))))
