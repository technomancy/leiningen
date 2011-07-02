;; The only requirement of the project.clj file is that it includes a
;; defproject form. It can have other code in it as well, including
;; loading other task definitions.

(defproject leiningen "1.6.1.20110701"
  :description "A build tool designed not to set your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License 1.0"}
  :disable-implicit-clean true
  :eval-in-leiningen true)
