(ns leiningen.core.test.classpath
  (:use [clojure.test]
        [leiningen.core.classpath])
  (:require [clojure.java.io :as io]
            [clojure.stacktrace :as stacktrace]
            [leiningen.core.user :as user]
            [leiningen.core.main :as main]
            [leiningen.test.helper :as lthelper]
            [leiningen.core.project :as project])
  (:import (java.io File)))

(use-fixtures :once
              (fn [f]
                ;; Can't have user-level profiles interfering!
                (with-redefs [user/profiles (constantly {})
                              user/credentials (constantly nil)]
                  (f))))

(defn m2-file [f]
  (io/file (System/getProperty "user.home") ".m2" "repository" f))

(def project {:managed-dependencies '[[org.clojure/clojure "1.3.0"]]
              :dependencies '[[org.clojure/clojure]
                              [ring/ring-core "1.0.0"
                               :exclusions [commons-codec]]]
              :checkout-deps-shares [:source-paths :resource-paths
                                     :compile-path #(lthelper/pathify (str (:root %) "/foo"))]
              :repositories project/default-repositories
              :root "/tmp/lein-sample-project"
              :target-path "/tmp/lein-sample-project/target"
              :source-paths ["/tmp/lein-sample-project/src"]
              :resource-paths ["/tmp/lein-sample-project/resources"]
              :test-paths ["/tmp/lein-sample-project/test"]})

(defn- resolve-with-repo [repo-url]
  (resolve-managed-dependencies :dependencies
                                :managed-dependencies
                                (assoc project
                                       :repositories {"repo" {:url repo-url}}
                                       :dependencies '[[lein-pprint "99"]])))

(deftest test-resolve-deps
  (doseq [f (reverse (file-seq (io/file (:root project))))]
    (when (.exists f) (io/delete-file f)))
  (testing "checks certificate expiry"
    (is (instance? java.security.cert.CertificateExpiredException
                   (try
                     (binding [main/*info* false]
                       (resolve-with-repo "https://expired.badssl.com/"))
                     (catch Exception e
                       (stacktrace/root-cause e))))))
  (testing "checks for host of cert"
    (let [ex (try
               (binding [main/*info* false]
                 (resolve-with-repo "https://badssl.f5n.de/"))
               (catch Exception e
                 (stacktrace/root-cause e)))]
      (is (or (instance? javax.net.ssl.SSLException ex)
              (instance? javax.net.ssl.SSLPeerUnverifiedException ex)
              (instance? java.security.GeneralSecurityException ex)))))
  (is (= #{(m2-file "org/clojure/clojure/1.3.0/clojure-1.3.0.jar")
           (m2-file "commons-io/commons-io/1.4/commons-io-1.4.jar")
           (m2-file "javax/servlet/servlet-api/2.5/servlet-api-2.5.jar")
           (m2-file "ring/ring-core/1.0.0/ring-core-1.0.0.jar")
           (m2-file (str "commons-fileupload/commons-fileupload/1.2.1/"
                         "commons-fileupload-1.2.1.jar"))}
         (set (resolve-managed-dependencies :dependencies
                                            :managed-dependencies
                                            project)))))

(deftest test-dependency-hierarchy
  (doseq [f (reverse (file-seq (io/file (:root project))))]
    (when (.exists f) (io/delete-file f)))
  (is (= '{[org.clojure/clojure "1.3.0"] nil
          [ring/ring-core "1.0.0"
           :exclusions [[commons-codec]]]
          {[commons-fileupload "1.2.1"] nil
           [commons-io "1.4"] nil
           [javax.servlet/servlet-api "2.5"] nil}}
         (managed-dependency-hierarchy :dependencies
                                       :managed-dependencies
                                       project))))

(def directories
  (vec (map lthelper/pathify
  ["/tmp/lein-sample-project/test"
   "/tmp/lein-sample-project/src"
   "/tmp/lein-sample-project/resources"])))

(def libs
  #{(str (m2-file "commons-io/commons-io/1.4/commons-io-1.4.jar"))
    (str (m2-file "javax/servlet/servlet-api/2.5/servlet-api-2.5.jar"))
    (str (m2-file "ring/ring-core/1.0.0/ring-core-1.0.0.jar"))
    (str (m2-file "commons-fileupload/commons-fileupload/1.2.1/commons-fileupload-1.2.1.jar"))
    (str (m2-file "org/clojure/clojure/1.3.0/clojure-1.3.0.jar"))})

(deftest test-classpath
  (let [classpath (get-classpath project)]
    (is (= (set classpath) (into libs directories)))
    (is (= directories (take 3 classpath)))
    (is (= libs (set (drop 3 classpath))))))

(defn canonical [& args]
  (.getCanonicalPath ^File (apply io/file args)))

(deftest test-checkout-deps
  (let [d1 (io/file (:root project) "checkouts" "d1")]
    (try
      (.mkdirs d1)
      (spit (io/file d1 "project.clj")
            (pr-str '(defproject hello "1.0")))
      (is (= (for [path ["src" "dev-resources" "resources" "target/classes" "foo"]]
               (lthelper/pathify (canonical "/tmp/lein-sample-project/checkouts/d1" path)))
             (#'leiningen.core.classpath/checkout-deps-paths project)))
      (finally
        ;; can't recur from finally
        (dorun (map #(.delete ^File %) (reverse (file-seq d1))))))))

(try
  ;; nio is required for symlinks but requires Java 7
  (import (java.nio.file Files Paths)
          (java.nio.file.attribute FileAttribute))
  (deftest test-checkout-symlink-unification
    (let [link! (fn link! [from to]
                  (java.nio.file.Files/createSymbolicLink (.toPath (io/file from))
                                            (.toPath (io/file to))
                                            (into-array java.nio.file.attribute.FileAttribute nil)))
          d1    (io/file (:root project) "deps" "d1")
          d2    (io/file (:root project) "deps" "d2")
          l1    (io/file (:root project) "checkouts" "d1")
          l2a   (io/file (:root project) "checkouts" "d2")
          l2b   (io/file d1 "checkouts" "d2")]
      (try
        (.mkdirs d1)
        (.mkdirs d2)
        (.mkdirs (io/file (:root project) "checkouts"))
        (.mkdirs (io/file d1 "checkouts"))
        (link! l1 d1)
        (link! l2a d1)
        (link! l2b (io/file "../../d2"))
        (spit (io/file d1 "project.clj")
              (str "(defproject p1 \"1.0\" :dependencies [[p2 \"1.0\"]] "
                   ":checkout-deps-shares ^:replace "
                   "[:source-paths #=(eval leiningen.core.classpath/checkout-deps-paths)])"))
        (spit (io/file d2 "project.clj")
              "(defproject p2 \"1.0\")")
        (is (= (for [dep  ["d1" "d2"]
                     path ["src"]]
                 (canonical (:root project) "deps" dep path))
               (#'leiningen.core.classpath/checkout-deps-paths
                 (assoc project :checkout-deps-shares
                                [:source-paths #'leiningen.core.classpath/checkout-deps-paths]))))
        (finally
          ;; can't recur from finally
          (doseq [d [d1 d2 l1 l2a l2b]
                  f (reverse (file-seq d))]
            (.delete ^File f)))
        )))
  (catch ClassNotFoundException ex
    nil))

(deftest test-add-auth
  (with-redefs [user/credentials (constantly
                                  {"https://sekrit.info/repo"
                                   {:username "milgrim" :password "reindur"}})
                user/profiles (constantly {:auth {:repository-auth
                                                  {#"clojars"
                                                   {:username "flynn"
                                                    :password "flotilla"}}}})]
    (is (= [["clojars" {:url "https://clojars.org/repo"
                        :username "flynn" :password "flotilla"}]
            ["sonatype" {:url "https://oss.sonatype.org/"}]
            ["internal" {:password "reindur" :username "milgrim"
                         :url "https://sekrit.info/repo"}]]
            (map add-repo-auth
                [["clojars" {:url "https://clojars.org/repo"}]
                  ["sonatype" {:url "https://oss.sonatype.org/"}]
                  ["internal" {:url "https://sekrit.info/repo"
                               :username :gpg :password :gpg}]])))))

(deftest test-normalize-dep-vectors
  (testing "dep vectors with string version"
    (is (= ['foo/bar "1.0.0"]
           (normalize-dep-vector ['foo/bar "1.0.0"])))
    (is (= ['foo/bar "1.0.0" :classifier "test"]
           (normalize-dep-vector ['foo/bar "1.0.0" :classifier "test"])))
    (is (= ['foo/bar "1.0.0" :classifier "test" :exclusions ['foo/baz]]
           (normalize-dep-vector ['foo/bar "1.0.0" :classifier "test" :exclusions ['foo/baz]]))))
  (testing "dep vectors with keyword version (e.g., for use with lein-modules)"
    (is (= ['foo/bar :version]
           (normalize-dep-vector ['foo/bar :version])))
    (is (= ['foo/bar :version :classifier "test"]
           (normalize-dep-vector ['foo/bar :version :classifier "test"])))
    (is (= ['foo/bar :version :classifier "test" :exclusions ['foo/baz]]
           (normalize-dep-vector ['foo/bar :version :classifier "test" :exclusions ['foo/baz]]))))
  (testing "dep vectors with explicit nils for versions (managed dependencies)"
    (is (= ['foo/bar nil]
           (normalize-dep-vector ['foo/bar nil])))
    (is (= ['foo/bar nil :classifier "test"]
           (normalize-dep-vector ['foo/bar nil :classifier "test"])))
    (is (= ['foo/bar nil :classifier "test" :exclusions ['foo/baz]]
           (normalize-dep-vector ['foo/bar nil :classifier "test" :exclusions ['foo/baz]]))))
  (testing "dep vectors with implicit nils for versions (managed dependencies)"
    (is (= ['foo/bar nil]
           (normalize-dep-vector ['foo/bar])))
    (is (= ['foo/bar nil :classifier "test"]
           (normalize-dep-vector ['foo/bar :classifier "test"])))
    (is (= ['foo/bar nil :classifier "test" :exclusions ['foo/baz]]
           (normalize-dep-vector ['foo/bar :classifier "test" :exclusions ['foo/baz]])))))
