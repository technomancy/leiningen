(ns leiningen.version
  "Print version for Leiningen and the current JVM.")

(defn ^:no-project-needed version
  "Print version for Leiningen and the current JVM."
  [project]
  (println "Leiningen" (System/getenv "LEIN_VERSION")
           "on Java" (System/getProperty "java.version")
           (System/getProperty "java.vm.name")))
