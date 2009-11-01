(ns leiningen.compile
  (:refer-clojure :exclude [compile]))

;; accept either a list of namespaces or spider the project root for .clj files
;; create classes/ directory

(defn compile [project])