(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require clojure.main
            [reply.main :as reply]
            [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.classpath :as classpath]))

(defn- start-server [project port]
  (eval/eval-in-project project
                        `(.get (nth (clojure.tools.nrepl/start-server ~port) 1))
                        '(require 'clojure.tools.nrepl)))

(defn get-port []
  (Integer.
   (or (System/getenv "LEIN_REPL_PORT")
       (dec (+ 1024 (rand-int 64512))))))

(defn repl
  ([] (repl nil))
  ([project]
     (let [port (get-port)]
       (future (start-server project port))
       (Thread/sleep 4000)
       (reply/launch-nrepl {:attach (str port)}))))
