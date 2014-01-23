(defproject middler "0.0.1"
  :description "Test some middleware."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :middleware [leiningen.core.test.project/add-seven])
