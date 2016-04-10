(def clj-version "1.3.0")

(defproject mgmt "0.99.0-SNAPSHOT"
  :description "A test project"

  :managed-dependencies [[~(symbol "org.clojure" "clojure") ~clj-version]
                 [rome ~(str "0." "9")]
                 [ring/ring "1.0.0"]]

  :dependencies [[org.clojure/clojure]
                 [rome/rome nil]
                 [ring]])
