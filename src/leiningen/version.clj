(ns leiningen.version
  "Print version for Leiningen and the current JVM.")

(defn version
  "Print version for Leiningen and the current JVM."
  []
  (println "Leiningen" (System/getenv "LEIN_VERSION")
           "on Java" (System/getProperty "java.version")
           (System/getProperty "java.vm.name")))
