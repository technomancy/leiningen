(defproject leiningen/lein-swank "1.2.0-SNAPSHOT"
  :description "A leiningen plugin to launch a swank server."
  :dependencies [[swank-clojure "1.2.0-SNAPSHOT"]
                 [ant/ant "1.6.5"]
                 [ant/ant-launcher "1.6.5"]
                 [org.apache.maven/maven-ant-tasks "2.0.10"]]
  :dev-dependencies [[org.clojure/clojure "1.1.0"]])
