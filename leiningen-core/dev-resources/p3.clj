(defproject middlest "0.0.1"
  :description "Test explicit middleware inside a plugin."
  :plugins [[lein-maven "0.1.0"]]
  :middleware [leiningen.mvn/maven-checkouts])
