(defproject project-name "1.0.0-SNAPSHOT"
  :description "Test support for transitive native dependencies"
  :native-path "nnnative"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [serial-port "1.0.7"]
                 [penumbra/lwjgl "2.4.2"]
                 [com.badlogicgames.gdx/gdx-platform "0.9.9"]
                 [com.badlogicgames.gdx/gdx-platform "0.9.9" :classifier "natives-desktop"]
                 [org.clojars.samaaron/rxtx "2.2.0"]
                 [jriengine "0.8.4"]
                 [tokyocabinet "1.24.0"]])
