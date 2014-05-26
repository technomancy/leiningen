(ns leiningen.test.change
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [leiningen.change :refer :all]
            [leiningen.release :as release]))

(deftest test-set-version
  (testing "project definition not found"
    (is (thrown-with-msg?
         IllegalArgumentException #"Project definition not found"
         (change-string ";;(defproject stealth.library \"0.0.0\")"
                        [:version] "set" "0.0.1"))))

  (testing "project version not found"
    (is (thrown-with-msg?
         IllegalArgumentException #"Project version not found"
         (change-string "(defproject com.someproject :dependencies [[\"some.thing\" \"2.3.1\"]])"
                        [:version] "set" "1.2.3"))))

  (testing "simplest possible case"
    (is (= "(defproject leingingen.change \"0.0.2-SNAPSHOT\")"
           (change-string "(defproject leingingen.change \"0.0.1\")"
                          [:version] "set" "0.0.2-SNAPSHOT"))))

  (testing "the largest project.clj in the repo"
    (let [before (slurp (clojure.java.io/resource "leiningen/help/project.clj"))
          after  (change-string before [:version] "set" "6.4.1")]
      ;; check the key portion
      (is (= "(defproject org.example/sample \"6.4.1\" " (.substring after 529 568)))
      ;; check a random dependency for changes
      (is (= "log4j \"1.2.15\"" (.substring after 2572 2586))))))

(deftest test-set-group-id
  (testing "renaming an existing group-id"
    (is (= "(defproject core/library \"0.0.1\" :license {})"
           (change-string "(defproject contrib/library \"0.0.1\" :license {})"
                          [:group-id] "set" "core"))))
  (testing "where group-id was previously implicit"
    (is (= "(defproject core/library \"0.0.1\" :license {})"
           (change-string "(defproject library \"0.0.1\" :license {})"
                          [:group-id] "set" "core")))))

(deftest test-set-artifact-id
  (testing "where group-id is implicit"
    (is (= "(defproject reagent \"0.0.1\" :license {})"
           (change-string "(defproject cloact \"0.0.1\" :license {})"
                          [:artifact-id] "set" "reagent"))))
  (testing "where group-id is explicit"
    (is (= "(defproject tonsky/datascript \"0.0.1\" :license {})"
           (change-string "(defproject tonsky/datalogscript \"0.0.1\" :license {})"
                          [:artifact-id] "set" "datascript")))))

(deftest test-external-function
  (testing "regular function by function reference"
    (is (= "(defproject leingingen.change \"1.9.53-SNAPSHOT\")"
           (change-string "(defproject leingingen.change \"1.9.52\")"
                          [:version] release/bump-version "patch"))))

  (testing "regular function by function reference"
    (is (= "(defproject leingingen.change \"1.9.52\")"
           (change-string "(defproject leingingen.change \"1.9.52-QUALIFIED\")"
                          [:version] "leiningen.release/bump-version" "release")))))

(deftest test-set-map-value
  (testing "can set a key"
    (is (= "(defproject leingingen.change \"0.0.1\" :description \"a dynamic description\")"
           (change-string "(defproject leingingen.change \"0.0.1\" :description \"a static description\")"
                          [:description] "set" "a dynamic description"))))

  (testing "can create a new key"
    (is (= "(defproject leingingen.change \"0.0.1\" :description \"a dynamic description\")"
           (change-string "(defproject leingingen.change \"0.0.1\")"
                          [:description] "set" "a dynamic description"))))

  (testing "can set a nested key"
    (is (= "(defproject leingingen.change \"0.0.1\" :license {:url \"http://example.com\"})"
           (change-string "(defproject leingingen.change \"0.0.1\" :license {:url \"http://old.com\"})"
                          [:license :url] "set" "http://example.com"))))

  (testing "can create a nested value"
    (is (= "(defproject leingingen.change \"0.0.1\" :a {:b {:c 1}})"
           (change-string "(defproject leingingen.change \"0.0.1\")"
                          [:a :b :c] "set" 1))))

  (testing "can understand cli short form"
    (is (= "(defproject leingingen.change \"0.0.1\" :license {:url \"http://example.com\"})"
           (change-string "(defproject leingingen.change \"0.0.1\")"
                          ":license:url" "set" "http://example.com")))))

(deftest test-normalize-path
  (is (= [:a]
         (normalize-path "a")
         (normalize-path ":a")))
  (is (= [:a :b]
         (normalize-path ":a:b")
         (normalize-path [:a :b]))))

(def div-dinc (comp inc inc /))

(deftest test-collapse-fn
  ;; right-partial application
  (is (= 10 ((collapse-fn div-dinc [2 3 4]) 192)))
  ;; return leading constant
  (is (= 10 ((collapse-fn "set" [10]) :ignored)))
  (is (= 10 ((collapse-fn "set" [10 :ignored :stuff]) nil)))
  ;; right-partial application + method lookup
  (is (= 10 ((collapse-fn #'leiningen.test.change/div-dinc [3]) 24))))
