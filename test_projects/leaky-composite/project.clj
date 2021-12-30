;; https://github.com/technomancy/leiningen/issues/2721
(defproject leaky-composite "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :profiles {:ring {:dependencies [[ring "1.8.2"]]}
             :dev  [:ring]})
