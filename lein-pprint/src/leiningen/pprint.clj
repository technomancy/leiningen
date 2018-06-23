(ns leiningen.pprint
  (:require [clojure.pprint :as pprint]
            [leiningen.core :refer [abort]]))

(defn ^:no-project-needed pprint
  "Usage: pprint [--not-pretty] [--] [selector...]

  When no selectors are specified, pretty-prints a representation of
  the entire project map.  Otherwise pretty-prints the item(s)
  retrieved by (get-in project selector) when reading a selector
  produces something sequential, or (get project selector) when it
  doesn't.  If \"--not-pretty\" is specified, doesn't pretty-print,
  just prints."
  [project & keys]
  (let [[pretty? keys] (loop [args args
                              pretty? true]
                         (if-let [[arg & args] (seq args)]
                           (case arg
                             "--" [pretty? args]
                             "--no-pretty" (recur false args)
                             (abort "Unrecognized argument" (pr-str arg)))))
        show (if pretty? pprint/pprint println)]
    (if (seq keys)
      (doseq [kstr keys]
        (let [k (read-string kstr)]
          (show (if (sequential? k)
                  (get-in project k)
                  (get project k)))))
      (show project)))
  (flush))
