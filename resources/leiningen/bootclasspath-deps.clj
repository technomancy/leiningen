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
        (-> (into (sorted-map-by (fn [x y]
                                   (compare (str x) (str y))))
                  (for [[a v] (artifacts hierarchy)]
                    [a v]))
            ;; Unhelpful to warn on these:
            (dissoc 'org.clojure/clojure)
            (dissoc 'leiningen-core)
            (pp/pprint))))
{clj-commons/pomegranate "1.2.23",
 com.hypirion/io "0.3.1",
 com.jcraft/jsch "0.1.55",
 com.jcraft/jsch.agentproxy.connector-factory "0.0.9",
 com.jcraft/jsch.agentproxy.core "0.0.9",
 com.jcraft/jsch.agentproxy.jsch "0.0.9",
 com.jcraft/jsch.agentproxy.pageant "0.0.9",
 com.jcraft/jsch.agentproxy.sshagent "0.0.9",
 com.jcraft/jsch.agentproxy.usocket-jna "0.0.9",
 com.jcraft/jsch.agentproxy.usocket-nc "0.0.9",
 commons-codec "1.15",
 commons-io "2.8.0",
 commons-lang "2.6",
 javax.inject "1",
 net.cgrand/parsley "0.9.3",
 net.cgrand/regex "1.1.0",
 net.java.dev.jna/jna "4.1.0",
 net.java.dev.jna/jna-platform "4.1.0",
 nrepl "1.0.0",
 org.apache.commons/commons-lang3 "3.12.0",
 org.apache.httpcomponents/httpclient "4.5.14",
 org.apache.httpcomponents/httpcore "4.4.16",
 org.apache.maven.resolver/maven-resolver-api "1.9.4",
 org.apache.maven.resolver/maven-resolver-connector-basic "1.9.4",
 org.apache.maven.resolver/maven-resolver-impl "1.9.4",
 org.apache.maven.resolver/maven-resolver-named-locks "1.9.4",
 org.apache.maven.resolver/maven-resolver-spi "1.9.4",
 org.apache.maven.resolver/maven-resolver-transport-file "1.9.4",
 org.apache.maven.resolver/maven-resolver-transport-http "1.9.4",
 org.apache.maven.resolver/maven-resolver-transport-wagon "1.9.4",
 org.apache.maven.resolver/maven-resolver-util "1.9.4",
 org.apache.maven.wagon/wagon-http "3.5.3",
 org.apache.maven.wagon/wagon-http-shared "3.5.3",
 org.apache.maven.wagon/wagon-provider-api "3.5.3",
 org.apache.maven.wagon/wagon-ssh "3.5.3",
 org.apache.maven.wagon/wagon-ssh-common "3.5.3",
 org.apache.maven/maven-artifact "3.8.7",
 org.apache.maven/maven-builder-support "3.8.7",
 org.apache.maven/maven-model "3.8.7",
 org.apache.maven/maven-model-builder "3.8.7",
 org.apache.maven/maven-repository-metadata "3.8.7",
 org.apache.maven/maven-resolver-provider "3.8.7",
 org.clojars.trptcolin/sjacket "0.1.1.1",
 org.clojure/core.specs.alpha "0.2.62",
 org.clojure/data.codec "0.1.0",
 org.clojure/data.xml "0.2.0-alpha6",
 org.clojure/spec.alpha "0.3.218",
 org.clojure/tools.macro "0.1.5",
 org.codehaus.plexus/plexus-interactivity-api "1.1",
 org.codehaus.plexus/plexus-interpolation "1.26",
 org.codehaus.plexus/plexus-utils "3.3.1",
 org.eclipse.sisu/org.eclipse.sisu.inject "0.3.5",
 org.flatland/classlojure "0.7.1",
 org.nrepl/incomplete "0.1.0",
 org.slf4j/jcl-over-slf4j "1.7.36",
 org.slf4j/slf4j-api "1.7.25",
 org.slf4j/slf4j-nop "1.7.25",
 org.tcrawley/dynapath "1.1.0",
 quoin "0.1.2",
 robert/hooke "1.3.0",
 scout "0.1.1",
 stencil "0.5.0",
 timofreiberg/bultitude "0.3.0"}
