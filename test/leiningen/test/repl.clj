(ns leiningen.test.repl
  (:require [clojure.test :refer :all]
            [leiningen.repl :refer :all]
            [leiningen.test.helper :as helper]
            [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [nrepl.ack :as ack]))

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

(deftest test-is-uri
  (is (= true  (is-uri? "http://example.org")))
  (is (= true  (is-uri? "https://example.org")))
  (is (= true  (is-uri? "http://example.org:20/repl")))
  (is (= true  (is-uri? "https://example.org:20/repl")))
  (is (= false (is-uri? "")))
  (is (= false (is-uri? "7")))
  (is (= false (is-uri? "myhost:9")))
  (is (= false (is-uri? "localhost:20")))
  (is (= false (is-uri? "localhost:"))))

(deftest test-connect-string
  (are [in exp]
       (= exp (with-redefs [repl-host (constantly "repl-host")
                            repl-port (constantly 5)]
                (connect-string {} [in])))
       ""                         "repl-host:5"
       "7"                        "repl-host:7"
       "myhost:9"                 "myhost:9"
       "http://localhost"         "http://localhost"
       "http://localhost/ham"     "http://localhost/ham"
       "http://localhost:20"      "http://localhost:20"
       "http://localhost:20/ham"  "http://localhost:20/ham"
       "https://localhost"        "https://localhost"
       "https://localhost/ham"    "https://localhost/ham"
       "https://localhost:20"     "https://localhost:20"
       "https://localhost:20/ham" "https://localhost:20/ham")
  (with-redefs [repl-host (constantly "repl-host")
                repl-port (constantly 0)]
    (is (= "repl-host:1" (connect-string {} ["1"])))
    (is (= "repl-host:123" (connect-string {} ["123"])))
    (are [in proj]
         (is (re-find
              #"Port is required"
              (helper/abort-msg connect-string proj in)))
         ["foo1234"]               {:root "/tmp"}
         []                        {:root "/tmp"}
         []                        helper/with-resources-project)
    (are [in proj]
         (is (re-find
              #"The file '.+' can't be read."
              (helper/abort-msg connect-string proj in)))
         ["@/tmp/please-do-not-create-this-file-it-will-break-my-test"] {}))
  (is (= "myhost:23" (connect-string helper/sample-project ["@test/sample-connect-string"])))
  (is (= "http://localhost:23/repl" (connect-string helper/sample-project ["@test/sample-connect-string-http"])))

  (is (= "127.0.0.1:4242" (connect-string helper/sample-project [])))
  (is (= "127.0.0.1:4343" (connect-string helper/sample-project ["4343"])))
  (is (= "127.0.0.1:4242" (connect-string helper/with-resources-project ["4242"]))))

(deftest test-options-for-reply
  (is (= (helper/fix-path-delimiters "/home/user/.lein-repl-history")
         (:history-file (options-for-reply {:root "/home/user"}))))
  (let [prompt-fn (fn [ns] "hi ")]
    (are
     [in exp]
     (= (merge
         {:history-file (helper/pathify
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
     [:port 3 :attach "xy"]          {:attach "xy"}
     [:attach "xy" :scheme "nrepl"]  {:attach "xy" :scheme "nrepl"}
     [:scheme "nrepl+edn"]           {:scheme "nrepl+edn"})))

(deftest test-init-ns
  (let [main {:main 'some.ns/main}
        repl-opts (merge main {:repl-options {:init-ns 'init-ns}})]
    (are [in exp] (= exp (init-ns in))
         main 'some.ns
         repl-opts 'init-ns)))

(defn- mocked-repl [& args]
  (with-redefs [ack/wait-for-ack (constantly 9999)
                resolve-reply-launch-nrepl (constantly identity)]
    (binding [main/*info* false]
      (apply repl args))))

(deftest test-scheme
  (let [project helper/sample-ordered-aot-project]
    (is (= {:attach "9999" :scheme "nrepl"}
           (-> (mocked-repl project)
               (select-keys [:attach :scheme]))))
    (is (= {:attach "9999" :scheme "nrepl"}
           (-> (mocked-repl project ":start"
                            ":transport" 'nrepl.transport/bencode)
               (select-keys [:attach :scheme]))))
    (is (= {:attach "9999" :scheme "nrepl+edn"}
           (-> (mocked-repl project ":start"
                            ":transport" 'nrepl.transport/edn)
               (select-keys [:attach :scheme]))))))
