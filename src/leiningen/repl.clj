(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require clojure.main
            [reply.main :as reply]
            [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [clojure.tools.nrepl :as nrepl]
            [leiningen.core.classpath :as classpath]))

(defn- start-server [project ack-port]
  (if project
    (eval/eval-in-project project
                          `(clojure.tools.nrepl/start-server 0 ~ack-port)
                          '(require 'clojure.tools.nrepl))
    (nrepl/start-server 0 ack-port)))

(def lein-repl-server (delay (nrepl/start-server)))

(defn ^:no-project-needed repl
  ([] (repl nil))
  ([project]
   (nrepl/reset-ack-port!)
   (.start
     (Thread.
       (bound-fn []
         (start-server project (-> @lein-repl-server first .getLocalPort)))))
   (reply/launch-nrepl {:attach (str (nrepl/wait-for-ack 20000))})))
