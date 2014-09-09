;; This project is used for leiningen's test suite, so don't change
;; any of these values without updating the relevant tests. If you
;; just want a basic project to work from, generate a new one with
;; "lein new".

(defproject nomnomnom "0.5.0-SNAPSHOT"
  :dependencies []
  :profiles {:default [:core-default :my-leaky :my-provided :my-test]
             :my-leaky ^:leaky {:dependencies
                                [[org.clojure/tools.macro "0.1.2"]]}
             :my-test
             ^{:pom-scope :test} {:dependencies
                                  [[org.clojure/java.classpath "0.2.2"]]}
             :my-provided
             ^{:pom-scope :provided} {:dependencies
                                      [[org.clojure/tools.namespace "0.2.6"]]}})
