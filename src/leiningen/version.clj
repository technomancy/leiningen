(ns leiningen.version
  "Print Leiningen's version to standard out.")

(defn version []
  (println "Leiningen" (System/getProperty "leiningen.version")))
