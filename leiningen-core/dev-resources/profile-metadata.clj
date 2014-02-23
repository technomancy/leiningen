(defproject metadata-check "0.1.0"
  :description "Check that profile metadata is retained."
  :license {:name "Eclipse Public License"}
  :dependencies [[leiningen-core "2.0.0-SNAPSHOT"]
                 [clucy "0.2.2" :exclusions [org.clojure/clojure]]
                 [lancet "1.0.1"]
                 [robert/hooke "1.1.2"]
                 [stencil "0.2.0"]]
  :profiles {:bar {:dependencies ^:please-keep-me [[lancet "1.0.2"]
                                                   [stencil "0.3.0"]]
                   :repositories ^:replace []}
             :baz {:dependencies ^:hello []
                   :repositories ^:displace []}})
