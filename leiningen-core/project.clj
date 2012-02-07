(defproject leiningen-core "2.0.0-SNAPSHOT"
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"}
  :description "Library for core functionality of Leiningen."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [classlojure "0.6.5"]
                 [robert/hooke "1.1.2"]
                 [com.cemerick/pomegranate "0.0.4"
                  :exclusions [org.slf4j/slf4j-api]]])
