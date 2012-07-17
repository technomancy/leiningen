(ns leiningen.repeat
  (:refer-clojure :exclude [repeat])
  (:require [leiningen.core.main :as main]))

(defn ^:no-project-needed ^:higher-order repeat
  "For when `lein clean && lein deps` just isn't enough.

USAGE: lein repeat 5 do deps, clean, compile, uberjar"
  [project num-times task-name & task-args]
  (dotimes [n (Integer/parseInt num-times)]
    (main/apply-task (main/lookup-alias task-name project) project task-args)))
