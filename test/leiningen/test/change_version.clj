(ns leiningen.test.change-version
  (require [clojure.test :refer :all]
           [leiningen.change-version :refer :all]))

(deftest test-change-version*
  (are [expected project-str new-version] (= expected (change-version* project-str new-version))
       "(defproject com.contentjon/hardware \"1.2.1\")"
       "(defproject com.contentjon/hardware \"1.0.1\")"
       "1.2.1"

       "; some comment
        (defproject com.contentjon/hardware \"1.2.1-SNAPSHOT\")"
       "; some comment
        (defproject com.contentjon/hardware \"1.0.1\")"
       "1.2.1-SNAPSHOT"

       "(defproject com.contentjon/hardware \"1.2.1\" ; version \"1.0.1-SNAPSHOT\")"
       "(defproject com.contentjon/hardware \"1.0.1-SNAPSHOT\" ; version \"1.0.1-SNAPSHOT\")"
       "1.2.1"

       "(defproject com.contentjon/hardware \"1.2.1\"
        :dependencies [[org.example/project \"1.0.1\"]])"
       "(defproject com.contentjon/hardware \"1.0.1\"
        :dependencies [[org.example/project \"1.0.1\"]])"
       "1.2.1"))
