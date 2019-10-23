(defproject middler-no-implicit-middleware "0.0.1"
  :description "Test some middleware."
  :middleware [leiningen.core.test.project/add-seven]
  :implicit-middleware false)
