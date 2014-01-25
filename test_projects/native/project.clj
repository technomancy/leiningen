(defproject project-name "1.0.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Test support for transitive native dependencies"
  :native-path "nnnative"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [serial-port "1.0.7"]
                 [penumbra/lwjgl "2.4.2"]
                 [org.clojars.samaaron/rxtx "2.2.0"]
                 [jriengine "0.8.4"]
                 [tokyocabinet "1.24.0"]])
