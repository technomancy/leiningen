(defproject org.domain/tricky-name "1.0"
  :description "One with a tricky group and project name"
  :dev-dependencies [[clojure "1.2.0"]]
  :shell-wrapper true
  :main ^{:skip-aot true} org.domain.tricky-name.core
  :run-aliases {:bbb org.domain.tricky-name.brunch
                :mmm org.domain.tricky-name.munch})
