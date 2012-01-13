(ns leiningen.pprint
  (:require [clojure.pprint :as pprint]))

(defn pprint
  "Pretty-print a representation of the project map."
  [project]
  (pprint/pprint project))