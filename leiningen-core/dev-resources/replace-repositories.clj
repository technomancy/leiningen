(defproject metadata-check "0.1.0"
  :description "Check that repositories can be replaced."
  :license {:name "Eclipse Public License"}
  :dependencies [[robert/hooke "1.1.2"]
                 [stencil "0.2.0"]]
  :repositories ^:replace [["nexus" {:url "https://clojars.org/repo/"}]])
