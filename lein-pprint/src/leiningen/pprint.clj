(ns leiningen.pprint
  (:require [clojure.pprint :as pprint]))

(defn ^:no-project-needed pprint
  "Pretty-print a representation of the project map."
  [project & keys]
  (if (seq keys)
    (doseq [k keys]
      (pprint/pprint (project (read-string k))))
    (pprint/pprint project))
  (flush))
