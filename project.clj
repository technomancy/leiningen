;; This is Leiningen's own project configuration. See doc/TUTORIAL.md
;; file as well as sample.project.clj for help writing your own.

(defproject leiningen "2.11.3-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :url "https://codeberg.org/leiningen/leiningen"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  ;; If you update these, update resources/leiningen/bootclasspath-deps.clj too
  :dependencies [[leiningen-core "2.11.3-SNAPSHOT"]
                 ;; needed for pom
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 ;; needed for test
                 [timofreiberg/bultitude "0.3.0"
                  :exclusions [org.clojure/clojure
                               org.tcrawley/dynapath]]
                 ;; needed for new
                 [stencil "0.5.0" :exclusions [org.clojure/core.cache]]
                 ;; needed for uberjar
                 [commons-lang "2.6"]
                 ;; needed for repl
                 [nrepl "1.3.0"]
                 ;; needed for change
                 [org.clojars.trptcolin/sjacket "0.1.1.1"
                  :exclusions [org.clojure/clojure]]
                 ;; bump versions of various common transitive deps
                 [net.cgrand/parsley "0.9.3" :exclusions [org.clojure/clojure]]
                 [scout "0.1.1"]
                 [commons-io "2.8.0"]
                 [commons-codec "1.15"]]
  :pedantic? :abort
  ;; checkout-deps don't work with :eval-in :leiningen
  :profiles {:dev {:resource-paths ["leiningen-core/dev-resources"]
                   :test-paths ["leiningen-core/test"]}
             :uberjar {:aot [#"leiningen"
                             leiningen.core.ssl ; lazy-loaded
                             cemerick.pomegranate
                             cemerick.pomegranate.aether
                             classlojure.core
                             dynapath.dynamic-classpath
                             dynapath.defaults
                             dynapath.util
                             bultitude.core
                             nrepl.core]}}
  :test-selectors {:default (complement :disabled)
                   :offline (comp (partial not-any? identity)
                                  (juxt :online :disabled))}
  :source-paths ["leiningen-core/src" "src"]
  :eval-in :leiningen)
    
