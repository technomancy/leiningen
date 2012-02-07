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

(defn- get-port []
  (Integer.
   (or (System/getenv "LEIN_REPL_PORT")
       (dec (+ 1024 (rand-int 64512))))))

(def retry-limit 200)

(defn- poll-repl-connection
  ([port retries]
     (when (> retries retry-limit)
       (throw (Exception. "Couldn't connect")))
     (Thread/sleep 100)
     (let [val (try (reply/launch-nrepl {:attach (str port)})
                    (catch java.net.ConnectException _ ::retry))]
       (if (= ::retry val)
         (recur port (inc retries))
         val)))
  ([port]
     (poll-repl-connection port 0)))

(defn repl
  ([] (repl nil))
  ([project]
     (let [port (get-port)]
       (future (start-server project port))
       (poll-repl-connection port))))
