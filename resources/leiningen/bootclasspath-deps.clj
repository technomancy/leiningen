;; This file is used to warn users when they attempt to load a plugin that
;; pulls in a dependency which conflicts with something already in use
;; by Leiningen itself.

;; This code regenerates the map
#_(do (require '[leiningen.core.project :as project])
      (require '[leiningen.core.classpath :as cp])
      (require '[clojure.pprint :as pp])

      (defn artifacts [h]
        (apply concat (keys h) (map artifacts (vals h))))

      (let [hierarchy (cp/managed-dependency-hierarchy :dependencies
                                                       :managed-dependencies
                                                       (project/read))]
        (-> (into {} (for [[a v] (artifacts h)]
                       [a v]))
            ;; Unhelpful to warn on these:
            (dissoc 'org.clojure/clojure)
            (dissoc 'leiningen-core)
            (pp/pprint))))
{
 bultitude "0.2.8"
 clojure-complete "0.2.4"
 com.cemerick/pomegranate "0.3.1"
 com.hypirion/io "0.3.1"
 commons-codec "1.9"
 commons-io "2.5"
 commons-lang "2.6"
 net.cgrand/parsley "0.9.3"
 net.cgrand/regex "1.1.0"
 net.cgrand/sjacket "0.1.1"
 org.apache.httpcomponents/httpclient "4.5.2"
 org.apache.httpcomponents/httpcore "4.4.4"
 org.apache.maven.wagon/wagon-http "2.12"
 org.apache.maven.wagon/wagon-http-shared "2.12"
 org.apache.maven.wagon/wagon-provider-api "2.2"
 org.apache.maven/maven-aether-provider "3.0.4"
 org.apache.maven/maven-model "3.0.4"
 org.apache.maven/maven-model-builder "3.0.4"
 org.apache.maven/maven-repository-metadata "3.0.4"
 org.clojure/data.xml "0.0.8"
 org.clojure/tools.macro "0.1.5"
 org.clojure/tools.nrepl "0.2.12"
 org.codehaus.plexus/plexus-classworlds "2.4"
 org.codehaus.plexus/plexus-component-annotations "1.5.5"
 org.codehaus.plexus/plexus-interpolation "1.14"
 org.codehaus.plexus/plexus-utils "3.0.24"
 org.flatland/classlojure "0.7.1"
 org.jsoup/jsoup "1.7.2"
 org.slf4j/jcl-over-slf4j "1.7.22"
 org.slf4j/slf4j-api "1.7.22"
 org.slf4j/slf4j-nop "1.7.22"
 org.sonatype.aether/aether-api "1.13.1"
 org.sonatype.aether/aether-connector-file "1.13.1"
 org.sonatype.aether/aether-connector-wagon "1.13.1"
 org.sonatype.aether/aether-impl "1.13.1"
 org.sonatype.aether/aether-spi "1.13.1"
 org.sonatype.aether/aether-util "1.13.1"
 org.sonatype.sisu/sisu-guice "3.0.3"
 org.sonatype.sisu/sisu-inject-bean "2.2.3"
 org.sonatype.sisu/sisu-inject-plexus "2.2.3"
 org.tcrawley/dynapath "0.2.5"
 pedantic "0.2.0"
 quoin "0.1.2"
 robert/hooke "1.3.0"
 scout "0.1.1"
 stencil "0.5.0"
 }
