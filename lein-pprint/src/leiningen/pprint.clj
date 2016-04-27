(ns leiningen.pprint
  (:require [clojure.pprint :as pprint]))

(defn ^:no-project-needed pprint
  "Pretty-print a representation of the project map."
  [project & keys]
  (if (seq keys)
    (doseq [kstr keys]
      (let [k (read-string kstr)]
        (pprint/pprint (if (sequential? k)
                         (get-in project k)
                         (get project k)))))
    (pprint/pprint project))
  (flush))
