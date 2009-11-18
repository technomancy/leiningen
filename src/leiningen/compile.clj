(ns leiningen.compile
  "Compile the namespaces listed in project.clj or all namespaces in src."
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]])
  (:refer-clojure :exclude [compile]))

(defn compile [project]
  ;; TODO: use a java subprocess in case a different clojure version is needed
  (doseq [n (or (:namespaces project)
                (find-namespaces-in-dir (file (:root project) "src")))]
    (println "Compiling" n)
    ;; TODO: check to see if bytecode is older than source
    (clojure.core/compile n)))
