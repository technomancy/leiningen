(defproject leiningen-core "2.5.0"
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Library for core functionality of Leiningen."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [bultitude "0.2.6"]
                 [classlojure "0.6.6"]
                 [robert/hooke "1.3.0"]
                 [com.cemerick/pomegranate "0.3.0"]
                 [org.apache.maven.wagon/wagon-http "2.4"]
                 [com.hypirion/io "0.3.1"]
                 [pedantic "0.2.0"]]
  :scm {:dir ".."}
  :dev-resources-path "dev-resources"
  :aliases {"bootstrap" ["with-profile" "base"
                         "do" "install," "classpath" ".lein-bootstrap"]})
