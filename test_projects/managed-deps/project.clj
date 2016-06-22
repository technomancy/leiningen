(def clj-version "1.3.0")

(defproject mgmt "0.99.0-SNAPSHOT"
  :description "A test project"

  :managed-dependencies [[~(symbol "org.clojure" "clojure") ~clj-version]
                 [rome ~(str "0." "9")]
                 [ring/ring "1.0.0"]
                 [ring/ring-codec "1.0.1"]
                 [commons-math/commons-math "1.2" :classifier "sources"]
                 [ring/ring-defaults "0.2.1"]]

  :dependencies [[org.clojure/clojure]
                 [rome/rome nil]
                 [ring]
                 [ring/ring-codec nil :exclusions [commons-codec]]
                 [commons-codec "1.6"]
                 [commons-math nil :classifier "sources"]])
