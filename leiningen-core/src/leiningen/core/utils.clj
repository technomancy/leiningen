(ns leiningen.core.utils
  (:require [clojure.java.io :as io]))

(defn read-file
  "Read the contents of file if it exists."
  [file]
  (if (.exists file)
    (read-string (slurp file))))

(defn ns-exists? [namespace]
  (some (fn [suffix]
          (-> (#'clojure.core/root-resource namespace)
              (subs 1)
              (str suffix)
              io/resource))
        [".clj" (str clojure.lang.RT/LOADER_SUFFIX ".class")]))

(defn error [& args]
  (binding [*out* *err*]
    (apply println "Error:" args)))

(defn require-resolve
  "Resolve a fully qualified symbol by first requiring its namespace."
  ([sym]
     (when-let [ns (namespace sym)]
       (when (ns-exists? ns)
         (let [ns (symbol ns)]
           (when-not (find-ns ns)
             (require ns)))
         (resolve sym))))
  ([ns sym] (require-resolve (symbol ns sym))))
