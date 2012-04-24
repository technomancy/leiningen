(ns leiningen.core.test.project
  (:refer-clojure :exclude [read])
  (:use [clojure.test]
        [leiningen.core.project])
  (:require [leiningen.core.user :as user]
            [clojure.java.io :as io]))

(use-fixtures :once
              (fn [f]
                ;; Can't have user-level profiles interfering!
                (with-redefs [user/profiles (constantly {})]
                  (f))))

(def paths {:source-paths ["src"],
            :test-paths ["test"],
            :resource-paths ["dev-resources" "resources"],
            :compile-path "target/classes",
            :native-path "target/native",
            :target-path "target"})

(def expected {:name "leiningen", :group "leiningen",
               :version "2.0.0-SNAPSHOT",
               :url "https://github.com/technomancy/leiningen"

               :disable-implicit-clean true,
               :eval-in :leiningen,
               :license {:name "Eclipse Public License"}

               :dependencies '[[leiningen-core "2.0.0-SNAPSHOT"]
                               [clucy "0.2.2" :exclusions [org.clojure/clojure]]
                               [lancet "1.0.1"]
                               [robert/hooke "1.1.2"]
                               [stencil "0.2.0"]],
               :twelve 12 ; testing unquote

               :repositories {"central" {:url "http://repo1.maven.org/maven2"}
                              "clojars" {:url "http://clojars.org/repo/"}}})

(deftest test-read-project
  (let [actual (read (.getFile (io/resource "p1.clj")))]
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

(def test-profiles (atom {:qa {:resource-paths ["/etc/myapp"]}
                          :test {:resource-paths ["test/hi"]}
                          :tes :test
                          :dev {:test-paths ["test"]}}))

(deftest test-merge-profile-paths
  (with-redefs [default-profiles test-profiles]
    (is (= ["/etc/myapp" "test/hi" "blue-resources" "resources"]
           (-> {:resource-paths ["resources"]
                :profiles {:blue {:resource-paths ["blue-resources"]}}}
               (merge-profiles [:qa :tes :blue])
               :resource-paths)))
    (is (= ["/etc/myapp" "test/hi" "blue-resources"]
           (-> {:resource-paths ^:displace ["resources"]
                :profiles {:blue {:resource-paths ["blue-resources"]}}}
               (merge-profiles [:qa :tes :blue])
               :resource-paths)))
    (is (= ["replaced"]
           (-> {:resource-paths ["resources"]
                :profiles {:blue {:resource-paths ^:replace ["replaced"]}}}
               (merge-profiles [:blue :qa :tes])
               :resource-paths)))))

(deftest test-global-exclusions
  (is (= '[[org.clojure/clojure]
          [org.clojure/clojure pomegranate]
          [org.clojure/clojure]]
         (map #(:exclusions (apply hash-map %))
              (-> {:dependencies
                   '[[lancet "1.0.1"]
                     [leiningen-core "2.0.0-SNAPSHOT" :exclusions [pomegranate]]
                     [clucy "0.2.2" :exclusions [org.clojure/clojure]]]
                   :exclusions '[org.clojure/clojure]}
                  (merge-profiles [:default])
                  :dependencies)))))

(defn add-seven [project]
  (assoc project :seven 7))

(deftest test-middleware
  (is (= 7 (:seven (read (.getFile (io/resource "p2.clj")))))))

(deftest test-init-project
  (is (= 7 (:seven (-> (.getFile (io/resource "p3.clj"))
                     read
                     (merge-profiles [:middler]))))))
