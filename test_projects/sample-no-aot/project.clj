(defproject nomnomnom "0.5.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [janino "2.5.15"]]
  :uberjar-exclusions [#"DUMMY"]
  :test-selectors {:default (fn [m] (not (:integration m)))
                   :integration :integration
                   :int2 :int2
                   :no-custom (fn [m] (not (false? (:custom m))))})
