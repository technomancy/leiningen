(ns leiningen.compile
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]])
  (:refer-clojure :exclude [compile]))

(defn compile
  "Compile the namespaces specified in build.clj or all namespaces in src/
  if none are provided."
  [project]
  ;; TODO: not sure why this is necessary
  (binding [*compile-path* "classes/"]
    (.mkdir (file (:root project) "classes"))
    (doseq [n (or (:namespaces project)
                  (find-namespaces-in-dir (file (:root project) "src")))]
      (println "Compiling" n)
      ;; TODO: check to see if bytecode is older than source
      (clojure.core/compile n))))
