(defproject reflector "0.1.0-SNAPSHOT"
  :description "a sample project involving AOT"
  :url "https://leiningen.org"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :aot [reflector.classy])
