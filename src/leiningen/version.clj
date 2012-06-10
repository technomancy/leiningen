(ns leiningen.version
  "Print version for Leiningen and the current JVM."
  (:require [leiningen.core.main :as main]))

(defn ^:no-project-needed version
  "Print version for Leiningen and the current JVM."
  [project]
  (println "Leiningen" (main/leiningen-version)
           "on Java" (System/getProperty "java.version")
           (System/getProperty "java.vm.name")))
