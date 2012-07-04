;; This is Leiningen's own project configuration. See doc/TUTORIAL.md
;; file as well as sample.project.clj for help writing your own.

(defproject leiningen "2.0.0-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[leiningen-core "2.0.0-SNAPSHOT"]
                 [clucy "0.3.0"]
                 [org.clojure/data.xml "0.0.3"]
                 [bultitude "0.1.7"]
                 [reply "0.1.0-beta8"]
                 ;; once reply bumps its drawbridge dep we can collapse these
                 [com.cemerick/drawbridge "0.0.6" :exclusions [ring/ring-core]]
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
