(ns leiningen.update-in
  "Perform arbitrary transformations on your project map."
  (:refer-clojure :exclude [update-in])
  (:require [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [leiningen.core.utils :as utils]
            [clojure.core :as clj]))

(defn ^:internal parse-args [key-path f args]
  (let [[f-args [_ & task+args]] (split-with #(not= "--" %) args)]
    [(mapv keyword (rest (.split key-path ":")))
     (utils/require-resolve (read-string f))
     (mapv read-string f-args)
     task+args]))

(defn ^:internal update-project [project keys-vec f args]
  (let [f #(apply apply (concat (if (seq keys-vec)
                                  [clj/update-in % keys-vec f]
                                  [f %])
                                args
                                [nil]))]
    (-> (vary-meta (f project) clj/update-in [:without-profiles] f)
        (project/load-plugins)
        (project/activate-middleware))))

(defn ^:higher-order ^:no-project-needed update-in
  "Perform arbitrary transformations on your project map.

Acts a lot like calling `clojure.core/update-in` on your project map
and then invoking a task on it, but with a few differences. Instead of
a vector of keys for reaching into nested maps, just mash keywords
together like \":repl-options:port\". A single \":\" refers to the map
root. Provide the arguments to f (which must be a resolvable var)
followed by \"--\", and then the task name and arguments to the task:

    $ lein update-in :dependencies conj \"[slamhound \\\"1.1.3\\\"]\" -- repl"
  [project key-path f & args]
  (let [[keys-vec f f-args task+args] (parse-args key-path f args)]
    (main/resolve-and-apply (update-project project keys-vec f f-args)
                            task+args)))
