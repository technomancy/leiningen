(ns leiningen.core.test.project
  (:refer-clojure :exclude [read])
  (:use [clojure.test]
        [leiningen.core.project])
  (:require [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath]
            [clojure.java.io :as io]))

(use-fixtures :once
              (fn [f]
                ;; Can't have user-level profiles interfering!
                (with-redefs [user/profiles (constantly {})
                              user/credentials (constantly nil)]
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
                              "clojars" {:url "https://clojars.org/repo/"}}})

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
                          :repl {:dependencies '[[org.clojure/tools.nrepl
                                                  "0.2.0-beta6"
                                                  :exclusions
                                                  [org.clojure/clojure]]
                                                 [org.thnetos/cd-client "0.3.4"
                                                  :exclusions
                                                  [org.clojure/clojure]]]}
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

(deftest test-merge-profile-deps
  (with-redefs [default-profiles test-profiles]
    (let [cp (-> {:resource-paths ["resources"]
                  :profiles {:dev {:dependencies
                                   '[^:displace [org.thnetos/cd-client "0.3.0"]
                                     [org.clojure/tools.nrepl "0.2.0-beta2"]]}}}
                 (merge-profiles [:dev :repl])
                 (classpath/get-classpath))]
      (is (some (partial re-find #"nrepl-0.2.0-beta2") cp))
      (is (some (partial re-find #"cd-client-0.3.4") cp)))))

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
  (is (= 7 (:seven (init-project (read (.getFile (io/resource "p2.clj"))))))))

(deftest test-add-profiles
  (let [expected-result {:dependencies [] :profiles {:a1 {:src-paths ["a1/"]}
                                                     :a2 {:src-paths ["a2/"]}}}]
    (is (= expected-result
           (-> {:dependencies []}
               (add-profiles {:a1 {:src-paths ["a1/"]}
                              :a2 {:src-paths ["a2/"]}}))))
    (is (= expected-result
           (-> {:dependencies []}
               (add-profiles {:a1 {:src-paths ["a1/"]}
                              :a2 {:src-paths ["a2/"]}})
               meta
               :without-profiles)))))

(deftest test-merge-anon-profiles
  (let [expected-result {:A 1 :C 3 :profiles {:a {:A 1}
                                              :b {:B 2}}
                         :repositories {"central" {:url "http://repo1.maven.org/maven2"}
                                        "clojars" {:url "https://clojars.org/repo/"}}
                         :dependencies [], :compile-path "classes"}]
    (is (= expected-result
           (-> {:profiles {:a {:A 1} :b {:B 2}}}
               (merge-profiles [:a {:C 3}]))))))

(deftest test-composite-profiles
  (let [expected-result {:A '(2 3 1), :B 2, :C 3,
                         :repositories {"central" {:url "http://repo1.maven.org/maven2"}
                                        "clojars" {:url "https://clojars.org/repo/"}}
                         :dependencies [], :compile-path "classes"}]
    (is (= expected-result
           (-> {:profiles {:a [:c :b]
                           :b [:d {:A [1] :B 1 :C 1}]
                           :c {:A [2] :B 2}
                           :d {:A [3] :C 3}}}
               (merge-profiles [:a])
               (dissoc :profiles))))))

(deftest test-override-default
  (let [expected-result {:A 1, :B 2, :C 3
                         :repositories {"central" {:url "http://repo1.maven.org/maven2"}
                                        "clojars" {:url "https://clojars.org/repo/"}}
                         :dependencies [], :compile-path "classes"}]
    (is (= expected-result
           (-> {:profiles {:a {:A 1 :B 2}
                           :b {:B 2 :C 2}
                           :c {:C 3}
                           :default [:c :b :a]}}
               (merge-profiles [:default])
               (dissoc :profiles))))))

(deftest test-unmerge-profiles
  (let [expected-result {:A 1 :C 3 :profiles {:a {:A 1}
                                              :b {:B 2}
                                              :c {:C 3}}
                         :repositories {"central" {:url "http://repo1.maven.org/maven2"}
                                        "clojars" {:url "https://clojars.org/repo/"}}
                         :dependencies [], :compile-path "classes"}]
    (is (= expected-result
           (-> {:profiles {:a {:A 1}
                           :b {:B 2}
                           :c {:C 3}}}
               (merge-profiles [:a :b :c])
               (unmerge-profiles [:b]))))
    (is (= expected-result
           (-> {:profiles {:a {:A 1}
                           :b {:B 2}
                           :c {:C 3}}}
               (merge-profiles [:a :b :c {:D 4}])
               (unmerge-profiles [:b {:D 4}]))))))

(deftest test-dedupe-deps
  (is (= '[[org.clojure/clojure "1.4.0"]
           [org.clojure/clojure "1.3.0" :classifier "sources"]]
         (-> {:dependencies '[[org.clojure/clojure "1.4.0"]
                              [org.clojure/clojure "1.3.0" :classifier "sources"]
                              [org.clojure/clojure "1.3.0"]]}
             (normalize-deps)
             (:dependencies)))))