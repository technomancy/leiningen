(defproject profile-middler "2.0.0-SNAPSHOT"
  :description "Automate Clojure projects without setting your hair on fire."
  :profiles {:middler {:middleware [leiningen.core.test.project/add-seven]}})
