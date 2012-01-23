;; This is Leiningen's own project configuration. See doc/TUTORIAL.md
;; file as well as sample.project.clj for help writing your own.

(defproject leiningen "2.0.0-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"}
  :dependencies [[leiningen-core "2.0.0-SNAPSHOT"]
                 [clucy "0.2.2"]
                 [stencil "0.2.0"]
                 [useful "0.7.6-alpha1"]
                 [org.clojars.ninjudd/data.xml "0.0.1-SNAPSHOT"]]
  :test-selectors {:default (complement :busted)}
  :eval-in-leiningen true)

;;; Release Checklist

;; * update NEWS, bin/lein, bin/lein.bat, project.clj, pom
;; * rm -rf lib classes, compile :all, generate uberjar, upload
;; * test self-install
;; * git tag
;; * push, push tags, update stable branch
;; * announce on mailing list
;; * bump version numbers (bin/lein, bin/lein.bat, project.clj)
;; * regenerate pom.xml
