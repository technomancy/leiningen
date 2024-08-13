(defproject provided "0"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :java-source-paths ["src"]
  :main provided.core.Example
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.4.0"]]}})
