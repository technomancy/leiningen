(def clj-version "1.3.0")

(defproject mgmt "0.99.0"
  :description "A test project"

  :managed-dependencies [[~(symbol "org.clojure" "clojure") ~clj-version]
                 [rome ~(str "0." "9")]
                 [ring/ring "1.0.0"]
                 [ring/ring-codec "1.0.1"]
                 [ring/ring-headers "0.2.0"]
                 [commons-math/commons-math "1.2" :classifier "sources"]
                 [org.apache.commons/commons-csv "1.4" :classifier "sources"]
                 [ring/ring-defaults "0.2.1"]
                 [org.clojure/tools.reader "1.0.0-beta3"]]

  :dependencies [[org.clojure/clojure]
                 [rome/rome nil]
                 [ring]
                 [ring/ring-codec nil :exclusions [commons-codec]]
                 [ring/ring-headers :exclusions [ring/ring-core]]
                 [commons-codec "1.6"]
                 [commons-math nil :classifier "sources"]
                 [org.apache.commons/commons-csv :classifier "sources"]
                 [org.clojure/tools.emitter.jvm "0.1.0-beta5"] ; depends on tools.reader 0.8.5
                 [org.clojure/tools.namespace "0.3.0-alpha3"] ; depends on tools.reader 0.10.0
                 ]

  :profiles {:add-deps {:dependencies [[org.clojure/clojure]]}
             :replace-deps {:dependencies ^:replace [[org.clojure/clojure]]}})
