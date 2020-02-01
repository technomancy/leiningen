(ns leiningen.pprint
  (:require [clojure.pprint :as pprint]))

(defn ^:no-project-needed pprint
  "Usage: pprint [--no-pretty] [--] [selector...]

  When no selectors are specified, pretty-prints a representation of
  the entire project map.  Otherwise pretty-prints the item(s)
  retrieved by (get-in project selector) when reading a selector
  produces something sequential, or (get project selector) when it
  doesn't.  If \"--no-pretty\" is specified, doesn't pretty-print,
  just prints."
  [project & keys]
  (let [[args rest] (split-at 2 keys)
        [show keys] (if (= args ["--no-pretty" "--"])
                      [println rest]
                      [pprint/pprint keys])]
    (if (seq keys)
      (doseq [kstr keys]
        (let [k (read-string kstr)]
          (show (if (sequential? k)
                  (get-in project k)
                  (get project k)))))
      (show project)))
  (flush))
