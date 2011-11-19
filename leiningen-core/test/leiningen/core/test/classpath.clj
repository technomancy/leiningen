(ns leiningen.core.test.classpath
  (:use [clojure.test]
        [leiningen.core.classpath])
  (:require [clojure.java.io :as io]))

(defn m2-file [f]
  (io/file (System/getProperty "user.home") ".m2" "repository" f))

(def project {:dependencies '[[org.clojure/clojure "1.3.0"]
                              [ring/ring-core "1.0.0-RC1"]]
              :dev-dependencies '[[slamhound "1.2.0"]]
              :root "/tmp/lein-sample-project"
              :source-path "/tmp/lein-sample-project/src"
              :resources-path "/tmp/lein-sample-project/resources"
              :test-path "/tmp/lein-sample-project/test"})

(deftest test-resolve-deps
  (doseq [f (reverse (file-seq (io/file (:root project))))]
    (when (.exists f) (io/delete-file f)))
  (is (= #{(m2-file "org/clojure/clojure/1.3.0/clojure-1.3.0.jar")
           (m2-file "commons-io/commons-io/1.4/commons-io-1.4.jar")
           (m2-file "javax/servlet/servlet-api/2.5/servlet-api-2.5.jar")
           (m2-file "commons-codec/commons-codec/1.4/commons-codec-1.4.jar")
           (m2-file "ring/ring-core/1.0.0-RC1/ring-core-1.0.0-RC1.jar")
           (m2-file (str "commons-fileupload/commons-fileupload/1.2.1/"
                         "commons-fileupload-1.2.1.jar"))}
         (resolve-dependencies project)))
  (is (= #{(m2-file "slamhound/slamhound/1.2.0/slamhound-1.2.0.jar")}
         (resolve-dev-dependencies project)))
  (is (.exists (io/file (:root project) "lib/dev" "slamhound-1.2.0.jar"))))

(def classpath
  ["/tmp/lein-sample-project/test"
   "/tmp/lein-sample-project/src"
   "/tmp/lein-sample-project/resources"
   "/home/phil/.m2/repository/slamhound/slamhound/1.2.0/slamhound-1.2.0.jar"
   "/home/phil/.m2/repository/commons-io/commons-io/1.4/commons-io-1.4.jar"
   "/home/phil/.m2/repository/javax/servlet/servlet-api/2.5/servlet-api-2.5.jar"
   "/home/phil/.m2/repository/commons-codec/commons-codec/1.4/commons-codec-1.4.jar"
   "/home/phil/.m2/repository/ring/ring-core/1.0.0-RC1/ring-core-1.0.0-RC1.jar"
   (str "/home/phil/.m2/repository/commons-fileupload/commons-fileupload/1.2.1/"
        "commons-fileupload-1.2.1.jar")
   "/home/phil/.m2/repository/org/clojure/clojure/1.3.0/clojure-1.3.0.jar"])

(deftest test-classpath
  ;; Can't test user plugins because anything could be installed.
  (with-redefs [leiningen.core.user/plugins (constantly [])]
    (is (= classpath (get-classpath project)))))

(deftest test-checkout-deps)