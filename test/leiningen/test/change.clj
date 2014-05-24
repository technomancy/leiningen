(ns leiningen.test.change
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [leiningen.change :refer :all]))

(defn bump-version [version]
  (let [[major minor patch meta] (str/split version #"\.|\-")
        new-patch (inc (Long/parseLong patch))]
    (format "%s.%s.%d-%s" major minor new-patch meta)))

(deftest test-set-version

  (testing "project definition not found"
    (is (thrown-with-msg?
         IllegalArgumentException #"Project definition not found"
         (change* ";;(defproject stealth.library \"0.0.0\")"
                  :version "0.0.1"))))

  (testing "project version not found"
    (is (thrown-with-msg?
         IllegalArgumentException #"Project version not found"
         (change* "(defproject com.someproject :dependencies [[\"some.thing\" \"2.3.1\"]])"
                  :version "1.2.3"))))

  (testing "simplest possible case"
    (is (= "(defproject leingingen.change \"0.0.2-SNAPSHOT\")"
           (change* "(defproject leingingen.change \"0.0.1\")"
                    :version "0.0.2-SNAPSHOT"))))

  (testing "the largest project.clj in the repo"
    (let [before (slurp (clojure.java.io/resource "leiningen/help/project.clj"))
          after  (change* before :version "6.4.1")]
      ;; check the key portion
      (is (= "(defproject org.example/sample \"6.4.1\" " (.substring after 529 568)))
      ;; check a random dependency for changes
      (is (= "log4j \"1.2.15\"" (.substring after 2572 2586))))))

(deftest test-external-function
  (testing "regular function by function reference"
    (is (= "(defproject leingingen.change \"1.9.53-SNAPSHOT\")"
           (change* "(defproject leingingen.change \"1.9.52-SNAPSHOT\")"
                    :version bump-version))))

    (testing "regular function by function reference"
    (is (= "(defproject leingingen.change \"1.9.53-SNAPSHOT\")"
           (change* "(defproject leingingen.change \"1.9.52-SNAPSHOT\")"
                    :version "leiningen.test.change/bump-version")))))

(deftest test-set-regular-key

  (testing "can set a key"
    (is (= "(defproject leingingen.change \"0.0.1\" :description \"a dynamic description\")"
           (change* "(defproject leingingen.change \"0.0.1\" :description \"a static description\")"
                    :description "a dynamic description"))))

  (testing "can create a new key"
    (is (= "(defproject leingingen.change \"0.0.1\" :description \"a dynamic description\")"
           (change* "(defproject leingingen.change \"0.0.1\")"
                    :description "a dynamic description")))))

(deftest test-nested-key

  (testing "can set a nested key"
    (is (= "(defproject leingingen.change \"0.0.1\" :license {:url \"http://example.com\"})"
           (change* "(defproject leingingen.change \"0.0.1\" :license {:url \"http://old.com\"})"
                    [:license :url] "http://example.com"))))

  (testing "can create a nested value"
    (is (= "(defproject leingingen.change \"0.0.1\" :a {:b {:c 1}})"
           (change* "(defproject leingingen.change \"0.0.1\")"
                    [:a :b :c] 1))))

    (testing "can understand cli short form"
    (is (= "(defproject leingingen.change \"0.0.1\" :license {:url \"http://example.com\"})"
           (change* "(defproject leingingen.change \"0.0.1\")"
                    :license:url "http://example.com")))))

(deftest test-normalize-path
  (is (= [:a]
         (normalize-path :a)
         (normalize-path "a")
         (normalize-path ":a")))
  (is (= [:a :b]
         (normalize-path :a:b)
         (normalize-path ":a:b")
         (normalize-path ["a" :b]))))

(def dinc (comp inc inc *))

(deftest test-collapse-fn
  ;; right-partial application
  (= 10 ((collapse-fn + [2 3 4]) 1))
  ;; return leading constant
  (= 10 ((collapse-fn 10 []) :ignored :stuff))
  (= 10 ((collapse-fn 10 [:ignored :stuff])))
  ;; right-partial application + method lookup
  (= 10 ((collapse-fn 'leiningen.test.change/dinc [4]) 2)))
