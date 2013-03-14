(ns leiningen.update
  (:require [leiningen.core.main :as main]))

(defn ^:higher-order update
  "Perform arbitrary transformations on your project map.

Functions a lot like clojure.core/update-in with a few differences.
Instead of a vector of keys, just mash them together like
\":repl-options:port\". Provide the arguments to f followed by \"--\",
and then the task name and arguments to the task."
  [project keys f & args]
  (let [keys-vec (map keyword (rest (.split keys ":")))
        update-args (map read-string (take-while (partial not= "--") args))
        [task-name & task-args] (drop (inc (count update-args)) args)
        f (resolve (read-string f))
        project (apply update-in project keys-vec f update-args)]
    (main/apply-task task-name project task-args)))
