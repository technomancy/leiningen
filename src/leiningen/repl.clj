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

(defn connect-to-repl [port]
  (Thread/sleep 3000) ;; TODO: poll
  (let [socket (Socket. "localhost" port)
        reader (InputStreamReader. (.getInputStream socket))
        writer (OutputStreamWriter. (.getOutputStream socket))
        copy-out #(do (while (.ready reader)
                         (Thread/sleep 100)
                         (.write *out* (.read reader)))
                       (flush))]
    (loop []
      (dotimes [_ 2] (copy-out))
      (.write writer (str (pr-str (read)) "\n"))
      (.flush writer)
      (recur))))

(defn repl [project]
  (let [port (+ 1024 (rand-int 32000))]
    (.start (Thread. #(eval-in-project project (repl-form project port))))
    (connect-to-repl port)))
