;; The only requirement of the project.clj file is that it includes a
;; defproject form. It can have other code in it as well, including
;; loading other task definitions.

(defproject leiningen "1.1.0-SNAPSHOT"
  :description "A build tool designed not to set your hair on fire."
  :url "http://github.com/technomancy/leiningen"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
                 [ant/ant "1.6.5"]
                 [ant/ant-launcher "1.6.5"]
                 [jline "0.9.94"]
                 [org.apache.maven/maven-ant-tasks "2.0.10"]]
  :dev-dependencies [[leiningen/lein-swank "1.0.0-SNAPSHOT"]
                     [autodoc "0.7.0"]]
  :main leiningen.core)
