(ns leiningen.version
  "Print Leiningen's version to standard out.")

(defn version [project]
  (println "Leiningen" (System/getProperty "leiningen.version")))
