(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require clojure.main
            [reply.main :as reply]
            [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [clojure.tools.nrepl :as nrepl]
            [leiningen.core.classpath :as classpath]))

(defn- start-server [project ack-port]
  (eval/eval-in-project project
                        `(clojure.tools.nrepl/start-server 0 ~ack-port)
                        '(require 'clojure.tools.nrepl)))

(defonce lein-repl-server (nrepl/start-server))

(defn repl
  ([] (repl nil))
  ([project]
   (nrepl/reset-ack-port!)
   (future (start-server project (-> lein-repl-server first .getLocalPort))) 
   (reply/launch-nrepl {:attach (str (nrepl/wait-for-ack 20000))})))
