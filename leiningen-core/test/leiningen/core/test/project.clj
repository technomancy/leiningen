(ns leiningen.core.test.project
  (:refer-clojure :exclude [read])
  (:use [clojure.test]
        [leiningen.core.project :as project])
  (:require [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.test.helper :refer [abort-msg]]
            [leiningen.test.helper :as lthelper]
            [leiningen.core.utils :as utils]
            [clojure.java.io :as io]))

(use-fixtures :once
              (fn [f]
                ;; Can't have user-level profiles interfering!
                (with-redefs [user/profiles (constantly {})
                              user/credentials (constantly nil)]
                  (f))))

(defn make-project
  "Make put a project map's :profiles on it's metadata"
  [m]
  (project-with-profiles-meta m (:profiles m)))

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

               :dependencies '[[leiningen-core/leiningen-core "2.0.0-SNAPSHOT"]
                               [clucy/clucy "0.2.2" :exclusions [[org.clojure/clojure]]]
                               [lancet/lancet "1.0.1"]
                               [robert/hooke "1.1.2"]
                               [stencil/stencil "0.2.0"]
                               [org.clojure/tools.nrepl "0.2.7"
                                :exclusions [[org.clojure/clojure]]
                                :scope "test"]
                               [clojure-complete/clojure-complete "0.2.3"
                                :exclusions [[org.clojure/clojure]]
                                :scope "test"]],
               :twelve 12 ; testing unquote

               :repositories [["central" {:url "https://repo1.maven.org/maven2/"
                                          :snapshots false}]
                              ["clojars" {:url "https://clojars.org/repo/"}]]})

(deftest test-read-project
  (let [actual (read (.getFile (io/resource "p1.clj")))]
    (doseq [[k v] expected]
      (is (= v (k actual))))
    (doseq [[k path] paths
            :when (string? path)]
      (is (= (lthelper/pathify (str (:root actual) "/" path))
             (k actual))))
    (doseq [[k path] paths
            :when (coll? path)]
      (is (= (for [p path] (lthelper/pathify (str (:root actual) "/" p)))
             (k actual))))))

;; TODO: test omit-default
;; TODO: test reading project that doesn't def project

(deftest test-replace-repositories
  (let [actual (read (.getFile (io/resource "replace-repositories.clj")))]
    (is (= 1 (-> actual :repositories count)))))

(deftest test-retain-profile-metadata
  (let [actual (read (.getFile (io/resource "profile-metadata.clj")))
        profiles (:profiles actual)]
    (is (true? (-> profiles :bar :dependencies meta :please-keep-me)))
    (is (true? (-> profiles :bar :repositories meta :replace)))
    (is (true? (-> profiles :baz :dependencies meta :hello)))
    (is (true? (-> profiles :baz :repositories meta :displace)))))

(deftest test-alias-in-profiles
  (let [actual (read (.getFile (io/resource "profile-metadata.clj")))]
    (is (= ["my" "java" "opts"]
           (-> actual :profiles :baz :jvm-opts)))))

(deftest test-merge-profile-displace-replace
  (let [test-profiles {:carmine {:foo [3 4]}
                       :carmined {:foo ^:displace [3 4]}
                       :carminer {:foo ^:replace [3 4]}
                       :blue {:foo [5 6]}
                       :blued {:foo ^:displace [5 6]}
                       :bluer {:foo ^:replace [5 6]}
                       :jade {:foo [7 8]}
                       :jaded {:foo ^:displace [7 8]}
                       :jader {:foo ^:replace [7 8]}}
        test-project (fn [p]
                       (project-with-profiles-meta
                         p
                         (merge test-profiles (:profiles p))))]
    (testing "that :^displace throws away the value if another exist"
      (is (= [1 2]
             (-> (make (test-project {:foo [1 2]}))
                 (merge-profiles [:carmined])
                 :foo)))
      (is (= [1 2 5 6]
             (-> (make (test-project {:foo [1 2]}))
                 (merge-profiles [:carmined :blue :jaded])
                 :foo)))
      (is (= [5 6]
             (-> (make (test-project {:foo ^:displace [1 2]}))
                 (merge-profiles [:carmined :blued])
                 :foo)))
      (is (= [7 8 5 6]
             (-> (make (test-project {:foo ^:displace [1 2]}))
                 (merge-profiles [:carmined :jade :blued :blue])
                 :foo))))
    (testing "that :^displace preserves metadata"
      (is (= {}
             (-> (make (test-project {:foo [1 2]}))
                 (merge-profiles [:carmined])
                 :foo meta)))
      (is (= {:quux :frob}
             (-> (make (test-project {:foo ^{:quux :frob} [1 2]}))
                 (merge-profiles [:carmined])
                 :foo meta)))
      (is (= {:displace true, :quux :frob}
             (-> (make (test-project
                        {:foo ^{:displace true, :quux :frob} [1 2]}))
                 (merge-profiles [:carmined :blued :jaded])
                 :foo meta)))
      (is (= {:displace true, :a 1, :b 2}
             (-> (make (test-project
                        {:foo ^{:displace true, :a 1} [1 2]
                         :profiles
                         {:bar {:foo
                                ^{:displace true, :b 2} [9 0]}}}))
                 (merge-profiles [:jaded :bar :carmined])
                 :foo meta))))
    (testing "that ^:replace replaces other values (at most once)"
      (is (= [1 2]
             (-> (make (test-project {:foo ^:replace [1 2]}))
                 (merge-profiles [:carmine])
                 :foo)))
      (is (= [3 4]
             (-> (make (test-project {:foo [1 2]}))
                 (merge-profiles [:carminer])
                 :foo)))
      (is (= [1 2 5 6]
             (-> (make (test-project {:foo ^:replace [1 2]}))
                 (merge-profiles [:carmine :blue])
                 :foo)))
      (is (= [3 4]
             (-> (make (test-project {:foo ^:replace [1 2]}))
                 (merge-profiles [:carminer])
                 :foo)))
      (is (= [7 8]
             (-> (make (test-project {:foo ^:replace [1 2]}))
                 (merge-profiles [:jader :blue])
                 :foo)))
      (is (= [3 4]
             (-> (make (test-project {:foo ^:replace [1 2]}))
                 (merge-profiles [:carminer :jade])
                 :foo))))
    (testing "that ^:replace preserves metadata"
      (is (= {}
             (-> (make (test-project {:foo [1 2]}))
                 (merge-profiles [:carminer])
                 :foo meta)))
      (is (= {:quux :frob}
             (-> (make (test-project {:foo ^{:quux :frob} [1 2]}))
                 (merge-profiles [:carminer])
                 :foo meta)))
      (is (= {:replace true, :quux :frob}
             (-> (make (test-project
                        {:foo ^{:replace true, :quux :frob} [1 2]}))
                 (merge-profiles [:carminer :jader :bluer])
                 :foo meta)))
      (is (= {:replace true, :a 1, :b 2}
             (-> (make (test-project
                        {:foo ^{:replace true, :a 1} [1 2]
                         :profiles {:bar {:foo
                                          ^{:replace true, :b 2} [9 0]}}}))
                 (merge-profiles [:jader :bar :carminer])
                 :foo meta))))
    (testing "that ^:displace and ^:replace operates correctly together"
      (is (= [5 6]
             (-> (make (test-project {:foo ^:displace [1 2]}))
                 (merge-profiles [:bluer])
                 :foo)))
      (is (= [1 2]
             (-> (make (test-project {:foo ^:replace [1 2]}))
                 (merge-profiles [:blued])
                 :foo)))
      (is (= [7 8]
             (-> (make (test-project {:foo [1 2]}))
                 (merge-profiles [:jader :carmined])
                 :foo)))
      (is (= [7 8]
             (-> (make (test-project {:foo [1 2]}))
                 (merge-profiles [:carmined :jader])
                 :foo))))
    (testing "that metadata is preserved at ^:displace/^:replace clashes"
      (is (= {:frob true}
             (-> (make (test-project
                        {:foo ^{:displace true, :frob true} [1 2]}))
                 (merge-profiles [:carminer])
                 :foo meta)))
      (is (= {:frob true}
             (-> (make (test-project
                        {:foo ^{:replace true, :frob true} [1 2]}))
                 (merge-profiles [:carmined])
                 :foo meta)))
      (is (= {:a 1, :b 2}
             (-> (make (test-project
                        {:foo ^{:replace true, :a 1} [1 2]
                         :profiles
                         {:bar {:foo ^{:displace true, :a 3, :b 2} [3 4]}}}))
                 (merge-profiles [:bar])
                 :foo meta)))
      (is (= {:a 3, :b 2}
             (-> (make (test-project
                        {:foo ^{:displace true, :a 1} [1 2]
                         :profiles
                         {:bar {:foo ^{:replace true, :a 3, :b 2} [3 4]}}}))
                 (merge-profiles [:bar])
                 :foo meta))))
    (testing "that built-in ^:replace values are properly replaced"
      (is (= '(constantly false)
             (-> (make {:test-selectors {:default '(constantly false)}})
                 (merge-profiles [:base])
                 :test-selectors :default))))
    (testing "that IObjs can be compared with non-IObjs without crashing"
      (is (= :keyword
             (-> (make {:test-selectors {:default :keyword}})
                 (merge-profiles [:base])
                 :test-selectors :default)))
      (is (= [1 2]
             (-> (make (test-project
                        {:foo ^:replace [1 2]
                         :profiles
                         {:bar {:foo 100}}}))
                 (merge-profiles [:bar])
                 :foo)))
      (is (= [1 2]
             (-> (make (test-project
                        {:foo 100
                         :profiles
                         {:bar {:foo ^:replace [1 2]}}}))
                 (merge-profiles [:bar])
                 :foo)))
      (is (= "string"
             (-> (make (test-project
                        {:foo "string"
                         :profiles
                         {:bar {:foo ^:displace [1 2]}}}))
                 (merge-profiles [:bar])
                 :foo)))
      (is (= "string"
             (-> (make (test-project
                        {:foo ^:displace [1 2]
                         :profiles
                         {:bar {:foo "string"}}}))
                 (merge-profiles [:bar])
                 :foo))))
    (testing "that IObjs keep their metadata when compared to non-IObjs"
      (is (= {:frob true}
             (-> (make (test-project
                        {:foo 100
                         :profiles
                         {:bar {:foo ^{:replace true, :frob true} [1 2]}}}))
                 (merge-profiles [:bar])
                 :foo meta))))))

(def test-profiles (atom {:qa {:resource-paths ["/etc/myapp"]}
                          :test {:resource-paths ["test/hi"]}
                          :repl {:dependencies
                                 '[[org.clojure/tools.nrepl "0.2.0-beta6"
                                    :exclusions [org.clojure/clojure]]
                                   [org.thnetos/cd-client "0.3.4"
                                    :exclusions [org.clojure/clojure]]]}
                          :tes :test
                          :dev {:test-paths ["test"]}}))

(deftest test-merge-profile-paths
  (let [test-project (fn [p]
                       (project-with-profiles-meta
                         p
                         (merge @test-profiles (:profiles p))))]
    (is (= (vec (map lthelper/fix-path-delimiters ["/etc/myapp" "test/hi" "blue-resources" "resources"]))
           (-> (make
                (test-project
                 {:resource-paths ["resources"]
                  :profiles {:blue {:resource-paths ["blue-resources"]}}}))
               (merge-profiles [:blue :tes :qa])
               :resource-paths)))
    (is (= (vec (map lthelper/fix-path-delimiters ["/etc/myapp" "test/hi" "blue-resources"]))
           (-> (make
                (test-project
                 {:resource-paths ^:displace ["resources"]
                  :profiles {:blue {:resource-paths ["blue-resources"]}}}))
               (merge-profiles [:blue :tes :qa])
               :resource-paths)))
    (is (= ["replaced"]
           (-> (make
                (test-project
                 {:resource-paths ["resources"]
                  :profiles {:blue {:resource-paths ^:replace ["replaced"]}}}))
               (merge-profiles [:tes :qa :blue])
               :resource-paths)))
    (is (= {:url "http://" :username "u" :password "p"}
           (-> (make
                (test-project
                 {:repositories [["foo" {:url "http://" :creds :gpg}]]
                  :profiles {:blue {:repositories {"foo"
                                                   ^:replace {:url "http://"
                                                              :username "u"
                                                              :password "p"}}}}}))
               (merge-profiles [:blue :qa :tes])
               :repositories
               last last)))))

(deftest test-merge-profile-deps
  (with-redefs [default-profiles test-profiles]
    (let [project (make
                   {:resource-paths ["resources"]
                    :dependencies '[^:displace [org.foo/bar "0.1.0" :foo [1 2]]
                                    [org.foo/baz "0.2.0" :foo [1 2]]
                                    [org.foo/zap "0.3.0" :foo [1 2]]]
                    :profiles {:dev {:dependencies
                                     '[[org.foo/bar "0.1.2"]
                                       [org.foo/baz "0.2.1"]
                                       ^:replace [org.foo/zap "0.3.1"]]}}})]
      (is (= '[[org.foo/bar "0.1.2"]
               [org.foo/baz "0.2.1" :foo [1 2]]
               [org.foo/zap "0.3.1"]]
             (-> (make-project project)
                 (merge-profiles [:dev])
                 :dependencies))))))

(deftest test-merge-profile-repos
  (with-redefs [default-profiles test-profiles]
    (let [project
          (make
           (make-project
            {:profiles {:clojars {:repositories ^:replace
                                  [["clojars.org" "https://clojars.org/repo/"]]}
                        :clj-2 {:repositories
                                [["clojars.org" "https://new-link.org/"]]}
                        :blue {:repositories
                               [["my-repo" "https://my-repo.org/"]]}
                        :red {:repositories
                              [^:replace ["my-repo" "https://my-repo.org/red"]]}
                        :green {:repositories
                                [^:displace
                                 ["my-repo" "https://my-repo.org/green"]]}
                        :empty {:repositories ^:replace []}}}))]
      (is (= default-repositories
             (:repositories project)))
      (is (= []
             (-> (merge-profiles project [:empty])
                 :repositories)))
      (is (= [["my-repo" {:url "https://my-repo.org/"}]]
             (-> (merge-profiles project [:empty :blue])
                 :repositories)))
      (is (= [["clojars.org" {:url "https://clojars.org/repo/"}]]
             (-> (merge-profiles project [:clojars])
                 :repositories)))
      (is (= [["clojars.org" {:url "https://clojars.org/repo/"}]
              ["my-repo" {:url "https://my-repo.org/"}]]
             (-> (merge-profiles project [:clojars :blue])
                 :repositories)))
      (is (= [["clojars.org" {:url "https://new-link.org/"}]
              ["my-repo" {:url "https://my-repo.org/"}]]
             (-> (merge-profiles project [:clojars :blue :clj-2])
                 :repositories)))
      (is (= [["clojars.org" {:url "https://clojars.org/repo/"}]
              ["my-repo" {:url "https://my-repo.org/"}]]
             (-> (merge-profiles project [:clojars :blue :green])
                 :repositories)))
      (is (= [["clojars.org" {:url "https://clojars.org/repo/"}]
              ["my-repo" {:url "https://my-repo.org/red"}]]
             (-> (merge-profiles project [:blue :clojars :red])
                 :repositories)))
      (is (= [["my-repo" {:url "https://my-repo.org/red"}]
              ["clojars.org" {:url "https://new-link.org/"}]]
             (-> (merge-profiles project [:empty :red :clj-2 :green])
                 :repositories))))))

(deftest test-merge-many-profiles
  (let [profiles (into {} (map #(vector (-> % str keyword) {:foo [%]}) (range 10)))
        project (make {:profiles profiles})]
    (is (= (range 10)
          (-> (make-project project)
            (merge-profiles [:0 :1 :2 :3 :4 :5 :6 :7 :8 :9])
            :foo)))))

(deftest test-global-exclusions
  (let [project {:dependencies
                 '[[lancet "1.0.1"]
                   [leiningen-core "2.0.0-SNAPSHOT" :exclusions [pomegranate]]
                   [clucy "0.2.2" :exclusions [org.clojure/clojure]]]
                 :exclusions '[org.clojure/clojure]}
        dependencies (:dependencies (merge-profiles project [:default]))]
    (is (= '[[[org.clojure/clojure]]
             [[org.clojure/clojure] [pomegranate/pomegranate]]
             [[org.clojure/clojure]]]
           (map #(distinct (:exclusions (apply hash-map %))) dependencies)))))

(defn add-seven [project]
  (assoc project :seven 7))

(deftest test-middleware
  (is (= 7 (:seven (init-project (read (.getFile (io/resource "p2.clj"))))))))

(deftest test-checkouts
  (let [project (read (.getFile (io/resource "p1.clj")))]
    (is (= #{"checkout-lib1" "checkout-lib2"} (set (map :name (read-checkouts project)))))))

(deftest test-activate-middleware
  (let [errors (atom [])]
    (with-redefs [utils/error (fn [& args] (swap! errors conj args))]
      (init-project (read (.getFile (io/resource "p3.clj")))))
    (is (= [] @errors))))

(deftest test-plugin-vars
  (are [project hooks middleware] (= (list hooks middleware)
                                     (map (partial plugin-vars project) [:hooks :middleware]))
       {:plugins '[[lein-foo "1.2.3"]]}
       '(lein-foo.plugin/hooks) '(lein-foo.plugin/middleware)

       {:plugins '[[lein-foo "1.2.3" :hooks false]]}
       '() '(lein-foo.plugin/middleware)

       {:plugins '[[lein-foo "1.2.3" :middleware false]]}
       '(lein-foo.plugin/hooks) '()

       {:plugins '[[lein-foo "1.2.3" :hooks false :middleware false]]}
       '() '()))

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
               :without-profiles)))
    (is (nil?
         (-> {:dependencies []}
             (add-profiles {:a1 {:src-paths ["a1/"]}
                            :a2 {:src-paths ["a2/"]}})
             :src-paths)))
    (is (= ["a1"]
           (-> {:dependencies []}
               (add-profiles {:a1 {:src-paths ["a1/"]}
                              :a2 {:src-paths ["a2/"]}})
               (merge-profiles [:a1])
               :src-paths)))))

(deftest test-merge-anon-profiles
  (is (= {:A 1, :C 3}
         (-> (make-project {:profiles {:a {:A 1} :b {:B 2}}})
             (merge-profiles [{:C 3} :a])
             (dissoc :profiles)))))

(deftest test-composite-profiles
  (is (= {:A '(1 3 2), :B 2, :C 3}
         (-> (make-project
              {:profiles {:a [:b :c]
                          :b [{:A [1] :B 1 :C 1} :d]
                          :c {:A [2] :B 2}
                          :d {:A [3] :C 3}}})
             (merge-profiles [:a])
             (dissoc :profiles)))))

(deftest test-override-default
  (is (= {:A 1, :B 2, :C 3}
         (-> (make-project
               {:profiles {:a {:A 1 :B 2}
                           :b {:B 2 :C 2}
                           :c {:C 3}
                           :default [:a :b :c]}})
             (merge-profiles [:default])
             (dissoc :profiles)))))

(deftest test-unmerge-profiles
  (let [expected {:A 1 :C 3}]
    (is (= expected
           (-> (make-project
                {:profiles {:a {:A 1}
                            :b {:B 2}
                            :c {:C 3}}})
               (merge-profiles [:a :b :c])
               (unmerge-profiles [:b])
               (dissoc :profiles))))
    (is (= expected
           (-> (make-project
                {:profiles {:a {:A 1}
                            :b {:B 2}
                            :c {:C 3}}})
               (merge-profiles [:a :b :c {:D 4}])
               (unmerge-profiles [:b {:D 4}])
               (dissoc :profiles))))
    (is (= expected
           (-> (make-project
                {:profiles {:a {:A 1}
                            :b {:B 2}
                            :c {:C 3}
                            :foo [:b]}})
               (merge-profiles [:a :b :c])
               (unmerge-profiles [:foo])
               (dissoc :profiles))))))

(deftest test-dedupe-deps
  (is (= '[[org.clojure/clojure "1.3.0"]
           [org.clojure/clojure "1.3.0" :classifier "sources"]]
         (-> (make
              {:dependencies '[[org.clojure/clojure "1.4.0"]
                               [org.clojure/clojure "1.3.0" :classifier "sources"]
                               [org.clojure/clojure "1.3.0"]]})
             (:dependencies)))))

(deftest test-dedupe-non-group-deps
  (is (= '[[foo/foo "1.1"]]
        (-> (make-project
              {:dependencies empty-dependencies
               :profiles {:a {:dependencies '[[foo "1.0"]]}
                          :b {:dependencies '[[foo "1.1"]]}}})
          (merge-profiles [:a :b])
          (:dependencies)))))

(deftest test-warn-user-repos
  (if (System/getenv "LEIN_SUPPRESS_USER_LEVEL_REPO_WARNINGS")
    (testing "no output with suppression"
      (is (= ""
             (abort-msg
              #'project/warn-user-repos
              {:user {:repositories
                      {"central" {:url "https://repo1.maven.org/maven2/"
                                  :snapshots false}
                       "clojars" {:url "https://clojars.org/repo/"}}}}))))
    (testing "with no suppression,"
      (testing "no warning without user level repo"
        (is (= "" (abort-msg #'project/warn-user-repos {}))
            "No warning in base case"))
      (testing "Warning with user level repo"
        (is (re-find
             #":repositories .* [:user].*"
             (abort-msg
              #'project/warn-user-repos
              {:user {:repositories
                      {"central" {:url "https://repo1.maven.org/maven2/"
                                  :snapshots false}
                       "clojars" {:url "https://clojars.org/repo/"}}}}))))
      (testing "Warning with user level repo"
        (is (re-find
             #":repositories .* [:user].*"
             (abort-msg
              #'project/warn-user-repos
              {:user {:repositories
                      {"central" "https://repo1.maven.org/maven2/"
                       "clojars" "https://clojars.org/repo/"}}}))))
      (testing "Warning with user level repo"
        (is (re-find
             #":repositories .* [:user].*"
             (abort-msg
              #'project/warn-user-repos
              {:user
               {:repositories
                [["central" {:url "https://repo1.maven.org/maven2/"
                             :snapshots false}]
                 ["clojars" {:url "https://clojars.org/repo/"}]]}})))))))
