(ns leiningen.repl
  (:require [clojure.main])
  (:use [leiningen.compile :only [eval-in-project]]
        [clojure.java.io :only [copy]])
  (:import [java.net Socket]
           [java.io OutputStreamWriter InputStreamReader]))

(defn repl-form [project port]
  (let [init-form (and (:main project)
                       [:init `#(doto '~(:main project) require in-ns)])]
    `(do (ns ~'user
           (:use [~'clojure.main :only [~'repl]])
           (:import [java.net ~'InetAddress ~'ServerSocket ~'Socket
                     ~'SocketException]
                    [java.io ~'InputStreamReader ~'OutputStream
                     ~'OutputStreamWriter ~'PrintWriter]
                    [clojure.lang ~'LineNumberingPushbackReader]))
         (println "Opening connection on port" ~port)
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

(defn- connect-to-repl [reader writer]
  (copy-out reader)
  (.write writer (str (pr-str (read)) "\n"))
  (.flush writer)
  (recur reader writer))

(defn- poll-for-socket [port]
  (Thread/sleep 100)
  (try (let [socket (Socket. "localhost" port)]
         (connect-to-repl (InputStreamReader. (.getInputStream socket))
                          (OutputStreamWriter. (.getOutputStream socket))))
       (catch java.net.ConnectException _))
  (recur port))

(defn repl [project]
  (let [port (+ 1024 (rand-int 32000))]
    (.start (Thread. #(eval-in-project project (repl-form project port))))
    (poll-for-socket port)))
