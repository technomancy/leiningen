(defproject nomnomnom "0.5.0-SNAPSHOT"
  :dependencies []
  :profiles {:default [:leiningen/default :my-leaky :my-provided :my-test]
             :my-leaky ^:leaky {:dependencies
                                [[org.clojure/tools.macro "0.1.2"]]}
             :my-test
             ^{:pom-scope :test} {:dependencies
                                  [[org.clojure/java.classpath "0.2.2"]]}
             :my-provided
             ^{:pom-scope :provided} {:dependencies
                                      [[org.clojure/tools.namespace "0.2.6"]]}})
