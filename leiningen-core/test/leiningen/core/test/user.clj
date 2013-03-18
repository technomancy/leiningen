(ns leiningen.core.test.user
  (:use clojure.test
        leiningen.core.user))

(deftest resolving-repo-creds
  (with-redefs [credentials (constantly {#"^https://clojars\.org/.*"
                                         {:username "u" :password "p"
                                          :passphrase "looooong"
                                          :private-key-file "./somewhere"}})]
    (testing "Literal creds unmolested"
      (is (= (resolve-credentials {:url "https://clojars.org/repo"
                                   :username "easily" :password "stolen"})
             {:url "https://clojars.org/repo"
              :username "easily" :password "stolen"})))
    (testing "Lookup in environment"
      (with-redefs [getenv {"LEIN_USERNAME" "flynn"
                            "CUSTOMENV" "flotilla"}]
        (is (= (resolve-credentials {:url "https://clojars.org/repo"
                                     :username :env
                                     :password :env/customenv})
               {:url "https://clojars.org/repo"
                :username "flynn" :password "flotilla"}))))
    (testing "Check multiple locations"
      (with-redefs [getenv {"LEIN_USERNAME" "flynn"
                            "CUSTOMENV" "flotilla"}]
        (is (= (resolve-credentials {:url "https://clojars.org/repo"
                                     :username [:gpg :env]
                                     :password [:env/customenv :gpg]})
               {:url "https://clojars.org/repo"
                :username "u" :password "flotilla"}))))
    (testing "Custom keys unmolested (and :creds expanded)"
      (is (= (resolve-credentials {:url "https://clojars.org/repo"
                                   :creds :gpg
                                   :foo [:gpg "0x00D85767"]})
             {:url "https://clojars.org/repo"
              :username "u" :password "p"
              :passphrase "looooong" :private-key-file "./somewhere"
              :foo [:gpg "0x00D85767"]})))))
