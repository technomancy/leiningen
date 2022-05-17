;; This is Leiningen's own project configuration. See doc/TUTORIAL.md
;; file as well as sample.project.clj for help writing your own.

(defproject leiningen "2.9.9-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;; If you update these, update resources/leiningen/bootclasspath-deps.clj too
  :dependencies [[leiningen-core "2.9.9-SNAPSHOT"]
                 ;; needed for pom
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 ;; needed for test
                 [timofreiberg/bultitude "0.3.0"
                  :exclusions [org.clojure/clojure]]
                 ;; needed for new
                 [stencil "0.5.0" :exclusions [org.clojure/core.cache]]
                 ;; needed for uberjar
                 [commons-lang "2.6"]
                 ;; needed for repl
                 [nrepl "0.9.0"]
                 ;; needed for change
                 [org.clojars.trptcolin/sjacket "0.1.1.1" :exclusions [org.clojure/clojure]]
                 ;; bump versions of various common transitive deps
                 [net.cgrand/parsley "0.9.3" :exclusions [org.clojure/clojure]]
                 [scout "0.1.1"]
                 [commons-io "2.8.0"]
                 [org.apache.httpcomponents/httpclient "4.5.13"]
                 ;; for PRISMA-2021-0055, dep: org.apache.httpcomponents/httpclient (patched above),
                 ;; which hasn't bumped commons-codec in its most recent version.
                 [commons-codec "1.15"]
                 [org.apache.httpcomponents/httpcore "4.4.13"]
                 ;; for CVE-2021-37714, dep chain: leiningen-core -> clj-commons/pomegranate ->
                 ;; org.apache.maven.wagon/wagon-http -> org.apache.maven.wagon/wagon-http-shared ->
                 ;; -> org.jsoup/jsoup
                 [org.jsoup/jsoup "1.14.2"]]
  :pedantic? :abort
  ;; checkout-deps don't work with :eval-in :leiningen
  :profiles {:dev {:resource-paths ["leiningen-core/dev-resources"]
                   :test-paths ["leiningen-core/test"]}
             :uberjar {:aot [#"leiningen"
                             leiningen.core.ssl ; lazy-loaded
                             cemerick.pomegranate
                             classlojure.core
                             nrepl.core]}}
  :test-selectors {:default (complement :disabled)
                   :offline (comp (partial not-any? identity)
                                  (juxt :online :disabled))}
  :source-paths ["leiningen-core/src" "src"]
  :eval-in :leiningen)
