(defproject middler-no-implicits "0.0.1"
  :description "Test some middleware."
  :middleware [leiningen.core.test.project/add-seven]
  :implicits false)
