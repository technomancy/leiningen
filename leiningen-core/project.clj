(defproject leiningen-core "2.8.1"
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Library for core functionality of Leiningen."
  ;; If you update these, update resources/leiningen/bootclasspath-deps.clj too
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [bultitude "0.2.8" :exclusions [org.tcrawley/dynapath]]
                 [org.flatland/classlojure "0.7.1"]
                 [robert/hooke "1.3.0"]
                 [com.cemerick/pomegranate "0.4.0"
                  :exclusions [org.tcrawley/dynapath
                               org.codehaus.plexus/plexus-utils]]
                 [org.tcrawley/dynapath "1.0.0"]
                 [org.apache.maven.wagon/wagon-http "2.12"
                  :exclusions [org.apache.maven.wagon/wagon-provider-api]]
                 [com.hypirion/io "0.3.1"]
                 [org.slf4j/slf4j-nop "1.7.22"] ; wagon-http uses slf4j
                 ;; we pull this in transitively but want a newer version
                 [org.clojure/tools.macro "0.1.5"]]
  :scm {:dir ".."}
  :dev-resources-path "dev-resources"
  :aliases {"bootstrap" ["with-profile" "base"
                         "do" "install," "classpath" ".lein-bootstrap"]})
