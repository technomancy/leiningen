(defproject leiningen-core "2.0.0-SNAPSHOT"
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Library for core functionality of Leiningen."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [bultitude "0.1.7"]
                 [classlojure "0.6.5"]
                 [robert/hooke "1.1.2"]
                 [ordered "1.3.1"]
                 [com.cemerick/pomegranate "0.0.13"
                  :exclusions [org.slf4j/slf4j-api]]]
  ;; This is only used when releasing Leiningen. Can't put it in a
  ;; profile since it must be installed using lein1
  ;;:aot :all
  :dev-resources-path "dev-resources"
  :profiles {:dev {:resource-paths ["dev-resources"]}})
