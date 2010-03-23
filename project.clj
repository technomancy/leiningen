;; The only requirement of the project.clj file is that it includes a
;; defproject form. It can have other code in it as well, including
;; loading other task definitions.

(defproject leiningen "1.2.0-SNAPSHOT"
  :description "A build tool designed not to set your hair on fire."
  :url "http://github.com/technomancy/leiningen"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [ant/ant "1.7.0"]
                 [jline "0.9.94"]
                 [org.apache.maven/maven-ant-tasks "2.1.0"]]
  :dev-dependencies [[leiningen/lein-swank "1.1.0"]
                     [autodoc "0.7.0"]]
  :main leiningen.core)
