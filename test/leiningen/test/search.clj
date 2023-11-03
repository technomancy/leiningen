(ns leiningen.test.search
  (:import (com.sun.net.httpserver HttpExchange HttpHandler HttpServer)
           (java.net InetSocketAddress))
  (:require [clojure.data.xml :as xml]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [leiningen.search :as search]))

;;; Setting up a mock server for querying

(def server (atom nil))

(defn start-server! [^InetSocketAddress sock]
  (let [mock-element  (xml/element "error"
                                   {:message "Invalid search syntax query"
                                    :id "24c1c717-98dd-4312-9fa8-8d63dab5ce74"})
        mock-error    (xml/emit-str mock-element)
        handle-search (reify HttpHandler
                        (^void handle [this ^HttpExchange exchange]
                         (with-open [response (.getResponseBody exchange)]
                           (.sendResponseHeaders exchange 400 (count mock-error))
                           (.write response (.getBytes ^String mock-error)))))]
    (reset! server (doto (HttpServer/create sock 0)
                     (.createContext "/search" handle-search)
                     (.start)))))

(defn stop-server! []
  (.stop ^HttpServer @server 0)
  (reset! server nil))

;;; Actual testing code

(use-fixtures :once
  (fn [f]
    (let [sock (InetSocketAddress. 50000)]
      (start-server! sock)
      (f)
      (stop-server!))))

(deftest parse-error-handling
  (testing "parse reports helpful errors"
    (let [url    "http://localhost:50000/search?q=foobar"
          result (with-out-str
                   ;; direct stderr to stdout so we can get a string and
                   ;; inspect it
                   (binding [*err* *out*]
                     (search/parse url)))]
      (is (str/includes? result "syntax unsupported")))))
