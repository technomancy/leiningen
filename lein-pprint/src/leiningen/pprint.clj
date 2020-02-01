(ns leiningen.pprint
  (:require [clojure.pprint :as pprint]))

(defn- parse-flags [args]
  (let [[flag separator & rest] args]
    (if (= [flag separator] ["--no-pretty" "--"])
      [println rest]
      [pprint/pprint args])))

(defn- get-values [project keys]
  (if (seq keys)
    (map #(let [key (read-string %)
                f (if (sequential? key) get-in get)]
            (f project key)) keys)
    [project]))

(defn ^:no-project-needed pprint
  "Usage: pprint [--no-pretty] [--] [selector...]

  When no selectors are specified, pretty-prints a representation of
  the entire project map.  Otherwise pretty-prints the item(s)
  retrieved by (get-in project selector) when reading a selector
  produces something sequential, or (get project selector) when it
  doesn't.  If \"--no-pretty\" is specified, doesn't pretty-print,
  just prints."
  [project & args]
  (let [[show keys] (parse-flags args)]
    (run! show (get-values project keys)))
  (flush))
