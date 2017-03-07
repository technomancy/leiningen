;; This is Leiningen's own project configuration. See doc/TUTORIAL.md
;; file as well as sample.project.clj for help writing your own.

(defproject leiningen "2.7.2-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[leiningen-core "2.7.2-SNAPSHOT"]
                 [org.clojure/data.xml "0.0.8"]
                 [commons-io "2.5"]
                 [commons-lang "2.6"]
                 [bultitude "0.2.8"]
                 [stencil "0.5.0" :exclusions [org.clojure/core.cache]]
                 [reply "0.3.7" :exclusions [ring/ring-core
                                             org.thnetos/cd-client]]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [clojure-complete "0.2.4"]
                 ;; bump versions of various common transitive deps
                 [scout "0.1.1"]
                 [slingshot "0.12.2"]
                 [cheshire "5.6.3"]
                 [clj-http "2.0.1"]
                 [net.cgrand/parsley "0.9.3" :exclusions [org.clojure/clojure]]]
  ;; checkout-deps don't work with :eval-in :leiningen
  :profiles {:dev {:resource-paths ["leiningen-core/dev-resources"]
                   :test-paths ["leiningen-core/test"]}
             :uberjar {:aot [#"leiningen"
                             leiningen.core.ssl ; lazy-loaded
                             cemerick.pomegranate
                             classlojure.core
                             clojure.tools.nrepl
                             clj-http.core]}}
  :test-selectors {:default (complement :disabled)
                   :offline (comp (partial not-any? identity)
                                  (juxt :online :disabled))}
  :source-paths ["leiningen-core/src" "src"]
  ;; work around Clojure bug http://dev.clojure.org/jira/browse/CLJ-1034
  :uberjar-exclusions [#"^data_readers.clj$"]
  :eval-in :leiningen)
