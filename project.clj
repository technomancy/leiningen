;; The only requirement of the project.clj file is that it includes a
;; defproject form. It can have other code in it as well, including
;; loading other task definitions.

(defproject leiningen "1.5.0-SNAPSHOT"
  :description "A build tool designed not to set your hair on fire."
  :url "https://github.com/technomancy/leiningen"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.apache.ant/ant "1.7.1"]
                 [robert/hooke "1.1.0"]
                 [org.apache.maven/maven-ant-tasks "2.0.10" :exclusions [ant]]]
  :dev-dependencies [[lein-deb "1.0.0-SNAPSHOT"]]
  :disable-implicit-clean true
  :eval-in-leiningen true
  :deb {:maintainer {:name "Travis Vachon"
                     :email "travis.vachon@gmail.com"}
        :depends "ant1.7, rlwrap, libjtidy-java"
        :filesets [{:dir "."
                    :includes "leiningen*.jar"
                    :prefix "/usr/share/java/lein"}
                   {:dir "lib"
                    :includes "hooke*.jar,clojure*.jar,maven*.jar,plexus*.jar,wagon*.jar,classworlds.jar"
                    :prefix "/usr/share/java/lein"}
                   {:file "bin/lein"
                    :fullpath "/usr/bin/lein"
                    :filemode "755"}]})
