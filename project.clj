;; This is Leiningen's own project configuration. See doc/TUTORIAL.md
;; file as well as sample.project.clj for help writing your own.

(defproject leiningen "2.0.0-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[leiningen-core "2.0.0-SNAPSHOT"]
                 [org.clojure/data.xml "0.0.3"]
                 [bultitude "0.1.7"]
                 [org.apache.maven.indexer/indexer-core "4.1.3"
                  :exclusions [org.apache.maven/maven-model
                               org.sonatype.aether/aether-api
                               org.sonatype.aether/aether-util]]
                 [reply "0.1.0-beta10"]
                 [clj-http "0.4.2"]]
  ;; checkout-deps don't work with :eval-in :leiningen
  :profiles {:dev {:resource-paths ["leiningen-core/dev-resources"]
                   :test-paths ["leiningen-core/test"]}
             :release {:aot [#"leiningen"
                             cemerick.pomegranate
                             cemerick.drawbridge
                             classlojure.core
                             clojure.tools.nrepl
                             clj-http.core
                             ordered.map]}}
  :test-selectors {:default (complement :post-preview)
                   :offline (complement :online)}
  :source-paths ["leiningen-core/src" "src"]
  :eval-in :leiningen)
