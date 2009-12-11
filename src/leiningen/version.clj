(ns leiningen.version
  "Print Leiningen's version to standard out.")

(defn version []
  (println "Leiningen" (System/getProperty "leiningen.version")
           "on Java" (System/getProperty "java.version")
           (System/getProperty "java.vm.name")))
