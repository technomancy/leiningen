(ns leiningen.test.change
  (:require [clojure.test :refer :all]
            [leiningen.change :refer :all]))

(deftest test-using-an-unknown-option
  (is (thrown-with-msg?
       IllegalArgumentException #"Do not currently support changing :language"
       (change* "(defproject com.someproject \"0.0.1\")"
                :language :reset-str "JAVA"))))

(deftest test-set-version

  (testing "project definition not found"
    (is (thrown-with-msg?
         IllegalArgumentException #"Project definition not found"
         (change* ";;(defproject stealth.library \"0.0.0\")"
                  :version :reset "0.0.1"))))

  (testing "project version not found"
    (is (thrown-with-msg?
         IllegalArgumentException #"Project version not found"
         (change* "(defproject com.someproject :dependencies [[\"some.thing\" \"2.3.1\"]])"
                  :version :reset "1.2.3"))))

  (testing "simplest possible case"
    (is (= "(defproject leingingen.change \"0.0.2-SNAPSHOT\")"
           (change* "(defproject leingingen.change \"0.0.1\")"
                    :version :reset "0.0.2-SNAPSHOT"))))

  (testing "the largest project.clj in the repo"
    (let [before (slurp (clojure.java.io/resource "leiningen/help/project.clj"))
          after  (change* before :version :reset "6.4.1")]
      ;; check the key portion
      (is (= "(defproject org.example/sample \"6.4.1\" " (.substring after 529 568)))
      ;; check a random dependency for changes
      (is (= "log4j \"1.2.15\"" (.substring after 2572 2586))))))

(deftest test-external-function
  (testing "regular function"
    (is (= "(defproject leingingen.change \"1.9.53-SNAPSHOT\")"
           (change* "(defproject leingingen.change \"1.9.52-SNAPSHOT\")"
                    :version :swap bump-version)))))
