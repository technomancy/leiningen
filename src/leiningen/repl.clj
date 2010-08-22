(ns leiningen.repl
  (:require [clojure.main])
  (:use [leiningen.compile :only [eval-in-project eval-without-project]]
        [clojure.java.io :only [copy]])
  (:import [java.net Socket]
           [java.io OutputStreamWriter InputStreamReader File]))

(defn repl-server [project host port]
  (let [init-form [:init `#(let [is# ~(:repl-init-script project)
                                 mn# '~(:main project)]
                             (when (and is# (.exists (File. str)))
                               (load-file is#))
                             (if mn#
                               (doto mn# require in-ns)
                               (in-ns '~'user)))]]
    `(do (ns ~'user
           (:require [~'clojure.java.shell])
           (:require [~'clojure.java.browse])
           (:use [~'clojure.main :only [~'repl]])
           (:import [java.net ~'InetAddress ~'ServerSocket ~'Socket
                     ~'SocketException]
                    [java.io ~'InputStreamReader ~'OutputStream
                     ~'OutputStreamWriter ~'PrintWriter]
                    [clojure.lang ~'LineNumberingPushbackReader]))
         (let [server# (ServerSocket. ~port 0 (~'InetAddress/getByName ~host))
               acc# (fn [s#]
                      (let [ins# (.getInputStream s#)
                            outs# (.getOutputStream s#)]
                        (doto (Thread.
                               #(binding [*in* (-> ins# InputStreamReader.
                                                   LineNumberingPushbackReader.)
                                          *out* (OutputStreamWriter. outs#)
                                          *err* (PrintWriter. outs# true)]
                                  (try
                                    (clojure.main/repl ~@init-form)
                                    (catch ~'SocketException _#
                                      (doto s#
                                        .shutdownInput
                                        .shutdownOutput
                                        .close)))))
                          .start)))]
           (doto (Thread. #(when-not (.isClosed server#)
                             (try
                               (acc# (.accept server#))
                               (catch ~'SocketException _#))
                             (recur)))
             .start)
           (format "REPL started; server listening on %s:%s." ~host ~port)))))

(defn copy-out [reader]
  (Thread/sleep 100)
  (.write *out* (.read reader))
  (while (.ready reader)
    (.write *out* (.read reader)))
  (flush))

(defn- repl-client [reader writer]
  (copy-out reader)
  (let [eof (Object.)
        input (read *in* false eof)]
    (when-not (= eof input)
      (.write writer (str (pr-str input) "\n"))
      (.flush writer)
      (recur reader writer))))

(defn- connect-to-server [socket]
  (repl-client (InputStreamReader. (.getInputStream socket))
               (OutputStreamWriter. (.getOutputStream socket))))

(defn- poll-for-socket [port]
  (Thread/sleep 100)
  (when (try (connect-to-server (Socket. "localhost" port))
             (catch java.net.ConnectException _ :retry))
    (recur port)))

(defn repl
  "Start a repl session. A socket-repl will also be launched in the
background; use the LEIN_REPL_PORT environment variable to set the port."
  ([] (repl {}))
  ([project]
    (let [host (or (System/getenv "LEIN_REPL_HOST") "localhost")
          port (Integer. (or (System/getenv "LEIN_REPL_PORT")
                             (dec (+ 1024 (rand-int 64512)))))
          server-form (repl-server project host port)
          server-thread (Thread. #(try (if (empty? project)
                                         (clojure.main/with-bindings
                                           (println (eval server-form)))
                                         (eval-in-project project server-form))
                                       (catch Exception _)))]
      (.start server-thread)
      (poll-for-socket port)
      (.stop server-thread))))
