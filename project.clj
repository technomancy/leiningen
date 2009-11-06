;; TODO: allow unqualified defproject
(leiningen.core/defproject leiningen
  :version "1.0-SNAPSHOT"
  :dependencies [["org.clojure" "clojure" "1.1.0-alpha-SNAPSHOT"]
                 ["org.clojure" "clojure-contrib" "1.0-SNAPSHOT"]
                 ["org.clojure" "lancet" "1.0-SNAPSHOT"]
                 ["org.apache.ant" "ant" "1.7.1"]
                 ["org.apache.ant" "ant-launcher" "1.7.1"]
                 ["org.apache.maven" "maven-ant-tasks" "2.0.10"]])

;; leiningen will define relevant deps, aot, jar, repl, etc tasks