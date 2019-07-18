(defproject custom/args "0.0.1-SNAPSHOT"
  :description "A test project"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:no-op {}
             :ascii {:jvm-opts ["-Dfile.encoding=ASCII"]}})
