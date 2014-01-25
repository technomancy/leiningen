(defproject middlest "0.0.1"
  :description "Test explicit middleware inside a plugin."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-maven "0.1.0"]]
  :middleware [leiningen.mvn/maven-checkouts])
