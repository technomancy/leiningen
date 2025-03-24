(defproject preserve-eval-meta "1.0"
  :description "Basic project with main invoked via lein run without reflection warnings"
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :global-vars {*warn-on-reflection* true}
  :preserve-eval-meta true
  :main ^{:skip-aot true} preserve-eval-meta.core)
