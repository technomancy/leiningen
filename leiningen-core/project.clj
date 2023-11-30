(defproject leiningen-core "2.10.1-SNAPSHOT"
  :url "https://codeberg.org/leiningen/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Library for core functionality of Leiningen."
  ;; If you update these, update resources/leiningen/bootclasspath-deps.clj too
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [timofreiberg/bultitude "0.3.0"
                  :exclusions [org.clojure/clojure
                               org.tcrawley/dynapath]]
                 [org.flatland/classlojure "0.7.1"]
                 [robert/hooke "1.3.0"]
                 [clj-commons/pomegranate "1.2.23"
                  :exclusions [org.slf4j/jcl-over-slf4j org.slf4j/slf4j-api
                               org.apache.maven.wagon/wagon-provider-api
                               org.apache.httpcomponents/httpcore
                               org.apache.httpcomponents/httpclient]]
                 [com.hypirion/io "0.3.1"]
                 [org.slf4j/slf4j-nop "1.7.25"] ; wagon-http uses slf4j
                 ;; we pull this in transitively but want a newer version
                 [org.clojure/tools.macro "0.1.5"]
                 ;; this new version doesn't bring in a dependency on
                 ;; an insecure version of jsoup
                 [org.apache.maven.wagon/wagon-http "3.5.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [commons-io/commons-io "2.6"]]
  :scm {:dir ".."}
  :dev-resources-path "dev-resources"
  :aliases {"bootstrap" ["with-profile" "base"
                         "do" "install," "classpath" ".lein-bootstrap"]})
