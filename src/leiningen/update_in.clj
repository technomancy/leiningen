(ns leiningen.update-in
  (:refer-clojure :exclude [update-in])
  (:require [leiningen.core.main :as main]))

(defn ^:higher-order update-in
  "Perform arbitrary transformations on your project map.

Acts a lot like calling `clojure.core/update-in` on your project map
and then invoking a task on it, but with a few differences. Instead of
a vector of keys for reaching into nested maps, just mash keywords
together like \":repl-options:port\". Provide the arguments to f
(which must be a resolvable var) followed by \"--\", and then the
task name and arguments to the task:

    $ lein update-in :dependencies conj \"[slamhound \\\"1.1.3\\\"]\" -- repl"
  [project keys f & args]
  (let [keys-vec (map keyword (rest (.split keys ":")))
        update-args (map read-string (take-while (partial not= "--") args))
        [task-name & task-args] (drop (inc (count update-args)) args)
        f (resolve (read-string f))
        project (apply update-in project keys-vec f update-args)]
    (main/apply-task task-name project task-args)))
