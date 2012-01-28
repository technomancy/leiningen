(ns leiningen.core.test.project
  (:refer-clojure :exclude [read])
  (:use [clojure.test]
        [leiningen.core.project])
  (:require [leiningen.core.user :as user]))

(use-fixtures :once
              (fn [f]
                ;; Can't have user-level profiles interfering!
                (with-redefs [user/profiles (constantly {})]
                  (f))))

(def paths {:source-path ["src"],
            :test-path ["test"],
            :resources-path ["dev-resources" "resources"],
            :compile-path "classes",
            :native-path "native",
            :target-path "target"})

(def expected {:name "leiningen", :group "leiningen",
               :version "2.0.0-SNAPSHOT",
               :url "https://github.com/technomancy/leiningen"

               :disable-implicit-clean true,
               :eval-in :leiningen,
               :license {:name "Eclipse Public License"}

               :dependencies '[[leiningen-core "2.0.0-SNAPSHOT"]
                               [clucy "0.2.2"] [lancet "1.0.1"]
                               [robert/hooke "1.1.2"]
                               [stencil "0.2.0"]],
               :twelve 12 ; testing unquote

               :repositories [["central" {:url "http://repo1.maven.org/maven2"}]
                              ["clojars" {:url "http://clojars.org/repo/"}]]})

(deftest test-read-project
  (let [actual (read "dev-resources/p1.clj")]
    (doseq [[k v] expected]
      (is (= (k actual) v)))
    (doseq [[k path] paths
            :when (string? path)]
      (is (= (k actual) (str (:root actual) "/" path))))
    (doseq [[k path] paths
            :when (coll? path)]
      (is (= (k actual) (for [p path] (str (:root actual) "/" p)))))))

;; TODO: test omit-default
;; TODO: test reading project that doesn't def project

(def test-profiles (atom {:qa {:resources-path ["/etc/myapp"]}
                          :test {:resources-path ["test/hi"]}
                          :tes :test
                          :dev {:test-path ["test"]}}))

(deftest test-merge-profile-paths
  (with-redefs [default-profiles test-profiles]
    (is (= ["/etc/myapp" "test/hi" "blue-resources" "resources"]
           (-> {:resources-path ["resources"]
                :profiles {:blue {:resources-path ["blue-resources"]}}}
               (merge-profiles [:qa :tes :blue])
               :resources-path)))
    (is (= ["/etc/myapp" "test/hi" "blue-resources"]
           (-> {:resources-path ^:displace ["resources"]
                :profiles {:blue {:resources-path ["blue-resources"]}}}
               (merge-profiles [:qa :tes :blue])
               :resources-path)))
    (is (= ["replaced"]
           (-> {:resources-path ["resources"]
                :profiles {:blue {:resources-path ^:replace ["replaced"]}}}
               (merge-profiles [:blue :qa :tes ])
               :resources-path)))))
