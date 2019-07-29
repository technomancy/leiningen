(ns leiningen.check
  "Check syntax and warn on reflection."
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [bultitude.core :as b]
            [clojure.java.io :as io]))

(defn check
  "Check syntax and warn on reflection."
  ([project]
     (let [source-files (map io/file (:source-paths project))
           nses (sort (b/namespaces-on-classpath :classpath source-files
                                                 :ignore-unreadable? false))
           action `(let [failures# (atom 0)]
                     (doseq [ns# '~nses]
                       ;; load will add the .clj, so can't use ns/path-for.
                       (let [ns-file# (-> (str ns#)
                                          (.replace \- \_)
                                          (.replace \. \/))]
                         (binding [*out* *err*]
                           (println "Compiling namespace" ns#))
                         (try
                           (binding [*warn-on-reflection* true]
                             (load ns-file#))
                           (catch ExceptionInInitializerError e#
                             (swap! failures# inc)
                             (.printStackTrace e#)))))
                     (if-not (zero? @failures#)
                       (System/exit @failures#)))]
       (try
         (binding [eval/*pump-in* false]
           (eval/eval-in-project project action))
         (catch clojure.lang.ExceptionInfo e
           (main/abort "Failed."))))))
