;; This is Leiningen's own project configuration. See doc/TUTORIAL.md
;; file as well as sample.project.clj for help writing your own.

(defproject leiningen "2.0.0-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[leiningen-core "2.0.0-SNAPSHOT"]
                 [clucy "0.2.3"]
                 [lein-newnew "0.2.7"]
                 [reply "0.1.0-beta5"]
                 [org.clojure/data.xml "0.0.3"]
                 [bultitude "0.1.5"]
                 [clj-http "0.3.6"]]
  ;; checkout-deps don't work with :eval-in :leiningen
  :profiles {:dev {:resource-paths ["leiningen-core/dev-resources"]
                   :test-paths ["leiningen-core/test"]}
             :release {:aot [#"leiningen"]}}
  :test-selectors {:default (complement :post-preview)
                   :offline (complement :online)}
  :source-paths ["leiningen-core/src" "src"]
  :eval-in :leiningen)

;;; Release Checklist

;; * update NEWS, bin/lein, bin/lein.bat, project.clj, leiningen-core/project.clj
;; * publish leiningen-core to clojars
;; * rm -rf target ~/.lein/self-installs/leiningen-*-SNAPSHOT-standalone.jar
;; * temporarily add :aot :all to leiningen-core/project.clj; lein install
;; * bin/lein uberjar, copy standalone to ~/.lein/self-installs
;; * ensure "time lein version" isn't bad
;; * upload to github
;; * test self-install
;; * git tag
;; * push, push tags, update stable branch
;; * announce on mailing list
;; * bump version numbers back to snapshot
;; * regenerate pom.xml
