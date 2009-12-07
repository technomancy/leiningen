(ns leiningen.version
  "Print leiningen's version to standard out.")

(defn version [project]
  (println "leiningen version:" (System/getProperty "leiningen.version")))
