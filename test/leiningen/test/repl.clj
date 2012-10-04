(ns leiningen.test.repl
  (:require [clojure.test :refer :all]
            [leiningen.repl :refer :all]
            [leiningen.core.user :as user]))

(def history-file (str (user/leiningen-home) "/repl-history"))

(deftest test-options-for-reply-empty
  (let [project {}]
    (is (= {:attach "127.0.0.1:9876"
            :custom-init nil
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-host
  (let [project {:repl-options {:host "192.168.0.10"}}]
    (is (= {:attach "192.168.0.10:9876"
            :host "192.168.0.10"
            :custom-init nil
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-prompt
  (let [prompt-fn (fn [ns] "hi ")
        project   {:repl-options {:prompt prompt-fn}}]
    (is (= {:attach "127.0.0.1:9876"
            :custom-prompt prompt-fn
            :custom-init nil
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-init
  (let [init-form '(println "ohai")
        project   {:repl-options {:init init-form}}]
    (is (= {:attach "127.0.0.1:9876"
            :custom-init init-form
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-init-ns
  (let [project {:repl-options {:init-ns 'foo.core}}]
    (is (= {:attach "127.0.0.1:9876"
            :init-ns 'foo.core
            :custom-init '(do (clojure.core/require 'foo.core)
                              (clojure.core/in-ns 'foo.core)
                              nil)
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-init-ns-and-init
  (let [project {:repl-options {:init-ns 'foo.core :init '(println "ohai")}}]
    (is (= {:attach "127.0.0.1:9876"
            :init-ns 'foo.core
            :custom-init '(do (clojure.core/require 'foo.core)
                              (clojure.core/in-ns 'foo.core)
                              (println "ohai"))
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-main-ns
  (let [project {:main 'foo.core}]
    (is (= {:attach "127.0.0.1:9876"
            :custom-init '(do (clojure.core/require 'foo.core)
                              (clojure.core/in-ns 'foo.core)
                              nil)
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-init-ns-beats-main
  (let [project {:main 'foo.core :repl-options {:init-ns 'winner.here}}]
    (is (= {:attach "127.0.0.1:9876"
            :init-ns 'winner.here
            :custom-init '(do (clojure.core/require 'winner.here)
                              (clojure.core/in-ns 'winner.here)
                              nil)
            :history-file history-file}
           (options-for-reply project :attach 9876)))))

