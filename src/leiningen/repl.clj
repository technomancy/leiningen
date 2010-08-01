(ns leiningen.repl
  (:require [clojure.main])
  (:use [leiningen.compile :only [eval-in-project]]
        [clojure.java.io :only [copy]])
  (:import [java.net Socket]
           [java.io OutputStreamWriter InputStreamReader File]))

(defn repl-server [project port]
  (let [init-form (and (or (:init-script project) (:main project))
                       [:init `#(let [is# ~(:init-script project)
                                      mn# '~(:main project)]
                                  (when (and is# (.exists (File. is#)))
                                    (load-file is#))
                                  (when mn# (doto mn# require in-ns)))])]
    `(do (ns ~'user
           (:use [~'clojure.main :only [~'repl]])
           (:import [java.net ~'InetAddress ~'ServerSocket ~'Socket
                     ~'SocketException]
                    [java.io ~'InputStreamReader ~'OutputStream
                     ~'OutputStreamWriter ~'PrintWriter]
                    [clojure.lang ~'LineNumberingPushbackReader]))
         (let [server# (ServerSocket. ~port)
               socket# (.accept server#)
               ins# (.getInputStream socket#)
               outs# (.getOutputStream socket#)]
           (binding [*in* (-> ins# InputStreamReader.
                              LineNumberingPushbackReader.)
                     *out* (OutputStreamWriter. outs#)
                     *err* (PrintWriter. outs# true)]
             (clojure.main/repl ~@init-form))
           (.close server#)))))

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
  "Start a repl session for the current project."
  [project]
  (let [port (dec (+ 1024 (rand-int 64512)))
        server-form (repl-server project port)
        server-thread (Thread. #(try (eval-in-project project server-form)
                                     (catch Exception _)))]
    (.start server-thread)
    (poll-for-socket port)
    (.stop server-thread)))
