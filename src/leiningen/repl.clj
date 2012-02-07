(ns leiningen.repl
  "Start a repl session either with the current project or standalone."
  (:require clojure.main
            [reply.main :as reply]
            [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [clojure.tools.nrepl :as nrepl]
            [leiningen.core.user :as user]
            [leiningen.core.classpath :as classpath]))

(defn- start-server [project port ack-port]
  (if project
    (eval/eval-in-project project
                          `(clojure.tools.nrepl/start-server ~port ~ack-port)
                          '(require 'clojure.tools.nrepl))
    (nrepl/start-server port ack-port)))

(def lein-repl-server (delay (nrepl/start-server)))

(defn ^:no-project-needed repl
  ([] (repl nil))
  ([project]
   (nrepl/reset-ack-port!)
   (.start
     (Thread.
       (bound-fn []
         (start-server project
                       (Integer.
                         (or (System/getenv "LEIN_REPL_PORT")
                             (:repl-port project)
                             0))
                       (-> @lein-repl-server first .getLocalPort)))))
   (reply/launch-nrepl
     (merge
       {:attach (str (nrepl/wait-for-ack (or (:repl-timeout project)
                                             (:repl-timeout (user/settings))
                                             30000)))}
       (:reply-options (user/settings))
       (:reply-options project)))))
