(defproject leiningen-core "2.8.2-SNAPSHOT"
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Library for core functionality of Leiningen."
  ;; If you update these, update resources/leiningen/bootclasspath-deps.clj too
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [bultitude "0.2.8" :exclusions [org.tcrawley/dynapath]]
                 [org.flatland/classlojure "0.7.1"]
                 [robert/hooke "1.3.0"]
                 [com.cemerick/pomegranate "1.0.0"
                  :exclusions [org.codehaus.plexus/plexus-utils
                               org.apache.maven.wagon/wagon-provider-api
                               org.apache.maven.wagon/wagon-http
                               org.apache.httpcomponents/httpclient
                               commons-codec]]
                 [org.tcrawley/dynapath "1.0.0"]
                 ;; Bumping this here until we get it fixed in pomegranate;
                 ;; see https://github.com/cemerick/pomegranate/pull/103
                 [org.apache.maven.wagon/wagon-provider-api "3.1.0"]
                 [org.apache.maven.wagon/wagon-http "3.1.0"
                  :exclusions [org.apache.httpcomponents/httpcore
                               org.apache.maven.wagon/wagon-provider-api]]
                 [org.apache.maven/maven-model-builder "3.5.3"]
                 [org.apache.maven/maven-model "3.5.3"]
                 [org.apache.maven/maven-repository-metadata "3.5.3"]
                 [com.hypirion/io "0.3.1"]
                 [org.slf4j/slf4j-nop "1.7.25"] ; wagon-http uses slf4j
                 ;; we pull this in transitively but want a newer version
                 [org.clojure/tools.macro "0.1.5"]]
  :scm {:dir ".."}
  :dev-resources-path "dev-resources"
  :aliases {"bootstrap" ["with-profile" "base"
                         "do" "install," "classpath" ".lein-bootstrap"]})
