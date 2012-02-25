(ns leiningen.check
  "Check syntax and warn on reflection."
  (:require [leiningen.core.eval :as eval]
            [bultitude.core :as b]
            [clojure.java.io :as io]))

(defn check
  "Check syntax and warn on reflection."
  ([project]
     (let [nses (b/namespaces-on-classpath :classpath (map io/file (:source-paths project)))
           action `(doseq [ns# '~nses]
                     ;; load will add the .clj, so can't use ns/path-for.
                     (let [ns-file# (-> (str ns#)
                                        (.replace \- \_)
                                        (.replace \. \/))]
                       (println "Compiling namespace" ns#)
                       (try
                         (binding [*warn-on-reflection* true]
                           (load ns-file#))
                         (catch ExceptionInInitializerError e#
                           (.printStackTrace e#)))))]
       (eval/eval-in-project project action))))
