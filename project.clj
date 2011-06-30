;; The only requirement of the project.clj file is that it includes a
;; defproject form. It can have other code in it as well, including
;; loading other task definitions.

(defproject leiningen "1.6.0"
  :description "A build tool designed not to set your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License 1.0"}
  :dependencies [;; TODO: declare these as .deb dependencies
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clucy "0.2.1"]
                 [lancet "1.0.1"]
                 [robert/hooke "1.1.2"]]
  :disable-implicit-clean true
  :eval-in-leiningen true)
