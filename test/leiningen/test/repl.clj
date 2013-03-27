(ns leiningen.test.repl
  (:require [clojure.test :refer :all]
            [leiningen.repl :refer :all]
            [leiningen.core.user :as user]))

(deftest test-opt-port
  (are [in exp] (= exp (opt-port in))
       [":port" "1"]        1
       [":foo" ":port" "1"] 1
       [":port" "1" ":foo"] 1
       ["1"]                nil))

(deftest test-repl-port
  (let [env "3"
        prj {:repl-options {:port 2}}
        prf {:user {:repl-options {:port 1}}}]
    (are [env proj prof exp]
         (= exp (with-redefs [user/getenv {"LEIN_REPL_PORT" env}
                              user/profiles (constantly prof)]
                  (repl-port proj)))
         env prj prf 3
         nil prj prf 2
         nil nil prf 1
         nil nil nil 0)))

(deftest test-repl-host
  (let [env "env-host"
        prj {:repl-options {:host "proj-host"}}
        prf {:user {:repl-options {:host "prof-host"}}}]
    (are [env proj prof exp]
         (= exp (with-redefs [user/getenv {"LEIN_REPL_HOST" env}
                              user/profiles (constantly prof)]
                  (repl-host proj)))
         env prj prf "env-host"
         nil prj prf "proj-host"
         nil nil prf "prof-host"
         nil nil nil "127.0.0.1")))

(deftest test-connect-string
  (are [in exp]
       (= exp (with-redefs [repl-host (constantly "repl-host")
                            repl-port (constantly 5)]
                (connect-string {} [in])))
       ""  "repl-host:5"
       "7" "repl-host:7"
       "myhost:9" "myhost:9"
       "http://localhost:20" "http://localhost:20"))

            :input-stream System/in}
           (options-for-reply project :attach 9876)))))
(deftest test-init-ns
  (let [main {:main 'main}
        repl-opts (merge main {:repl-options {:init-ns 'init-ns}})]
    (are [in exp] (= exp (init-ns in))
         main 'main
         repl-opts 'init-ns)))
