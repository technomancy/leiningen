(defproject org.domain/tricky-name "1.0"
  :description "One with a tricky group and project name"
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :global-vars {*warn-on-reflection* true}
  :main ^{:skip-aot true} org.domain.tricky-name.core)
