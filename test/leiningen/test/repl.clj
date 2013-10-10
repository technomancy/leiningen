(ns leiningen.test.repl
  (:require [clojure.test :refer :all]
            [leiningen.repl :refer :all]
            [leiningen.test.helper :as lthelper]
            (leiningen.core [user :as user] [project :as project])))

(deftest test-merge-repl-profile
  (is (= (-> {:repl-options {:ack-port 4}}
             (with-meta
               {:without-profiles {:repl-options {:ack-port 3}}
                :profiles {:repl {:repl-options {:ack-port 2}}
                           :user {:repl-options {:ack-port 1}}}
                :active-profiles [:default]})
             (project/merge-profiles [:repl])
             :repl-options :ack-port)
         2)))

(deftest test-opt-host
  (are [in exp] (= exp (opt-host in))
       [":host" "0.0.0.0"]        "0.0.0.0"
       [":host" "1.1.1.1"]        "1.1.1.1"
       [":foo" ":host" "0.0.0.0"] "0.0.0.0"
       [":host" "0.0.0.0" ":foo"] "0.0.0.0"
       ["0.0.0.0"]                nil
       [":host"]                  nil
       [":port" "0.0.0.0"]        nil
       []                         nil
       nil                        nil))

(deftest test-opt-port
  (are [in exp] (= exp (opt-port in))
       [":port" "1"]        1
       [":foo" ":port" "1"] 1
       [":port" "1" ":foo"] 1
       ["1"]                nil
       []                   nil))

(deftest test-ack-port
  (let [env "5"
        prj {:repl-options {:ack-port 4}}]
    (are [env proj exp]
         (= exp (with-redefs [user/getenv {"LEIN_REPL_ACK_PORT" env}]
                  (ack-port proj)))
         env prj 5
         nil prj 4
         nil nil nil)))

(deftest test-repl-port
  (let [env "3"
        prj {:repl-options {:port 2}}]
    (are [env proj exp]
         (= exp (with-redefs [user/getenv {"LEIN_REPL_PORT" env}]
                  (repl-port proj)))
         env prj 3
         nil prj 2
         nil nil 0)))

(deftest test-repl-host
  (let [env "env-host"
        prj {:repl-options {:host "proj-host"}}]
    (are [env proj exp]
         (= exp (with-redefs [user/getenv {"LEIN_REPL_HOST" env}]
                  (repl-host proj)))
         env prj "env-host"
         nil prj "proj-host"
         nil nil "127.0.0.1")))

(deftest test-connect-string
  (are [in exp]
       (= exp (with-redefs [repl-host (constantly "repl-host")
                            repl-port (constantly 5)]
                (connect-string {} [in])))
       ""                    "repl-host:5"
       "7"                   "repl-host:7"
       "myhost:9"            "myhost:9"
       "http://localhost:20" "http://localhost:20")
  (with-redefs [repl-host (constantly "repl-host")
                repl-port (constantly 0)]
    (is (= "repl-host:1" (connect-string {} ["1"])))
    (is (= "repl-host:123" (connect-string {} ["123"])))
    (is (re-find
         #"Port is required"
         (lthelper/abort-msg connect-string {} []))))
  (is (= "127.0.0.1:4242" (connect-string lthelper/sample-project [])))
  (is (= "127.0.0.1:4343" (connect-string lthelper/sample-project ["4343"])))
  (is (re-find
       #"Port is required"
       (lthelper/abort-msg connect-string lthelper/with-resources-project [])))
  (is (= "127.0.0.1:4242" (connect-string lthelper/with-resources-project ["4242"]))))

(deftest test-options-for-reply
  (is (= (lthelper/fix-path-delimiters "/home/user/.lein-repl-history")
         (:history-file (options-for-reply {:root "/home/user"}))))
  (let [prompt-fn (fn [ns] "hi ")]
    (are
     [in exp]
     (= (merge
         {:history-file (lthelper/pathify
                          (str (user/leiningen-home) "/repl-history"))
          :custom-help (list 'println (slurp (clojure.java.io/resource
                                               "repl-welcome")))
          :input-stream System/in}
         exp)
        (let [[prj-k prj-v arg-k arg-v] in]
          (apply options-for-reply
                 {:repl-options (into {} (and prj-k {prj-k prj-v}))}
                 (into [] (and arg-k [arg-k arg-v])))))
     [:standalone true]              {:standalone true}
     [:prompt prompt-fn]             {:custom-prompt prompt-fn}
     [:host "prj-host"]              {:host "prj-host"}
     [:host "prj-host" :port 1]      {:host "prj-host" :port "1"}
     [nil nil :port 1]               {:port "1"}
     [:port 2]                       {:port "2"}
     [:port 2 :port 1]               {:port "1"}
     [:host "prj-host" :attach "xy"] {:attach "xy"}
     [:port 3 :attach "xy"]          {:attach "xy"})))

(deftest test-init-ns
  (let [main {:main 'main}
        repl-opts (merge main {:repl-options {:init-ns 'init-ns}})]
    (are [in exp] (= exp (init-ns in))
         main 'main
         repl-opts 'init-ns)))
