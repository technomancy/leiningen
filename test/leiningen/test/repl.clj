(ns leiningen.test.repl
  (:require [clojure.test :refer :all]
            [leiningen.repl :refer :all]
            [leiningen.core.user :as user]))

(def history-file (str (user/leiningen-home) "/repl-history"))

(deftest test-options-for-reply-empty
  (let [project {}]
    (is (= {:attach "127.0.0.1:9876" :history-file history-file
            :input-stream System/in}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-host
  (let [project {:repl-options {:host "192.168.0.10"}}]
    (is (= {:attach "192.168.0.10:9876" :host "192.168.0.10"
            :history-file history-file :input-stream System/in}
           (options-for-reply project :attach 9876)))))

(deftest test-options-for-reply-prompt
  (let [prompt-fn (fn [ns] "hi ")
        project   {:repl-options {:prompt prompt-fn}}]
    (is (= {:attach "127.0.0.1:9876"
            :custom-prompt prompt-fn :history-file history-file
            :input-stream System/in}
           (options-for-reply project :attach 9876)))))

(deftest repl-profile-in-project
  (let [p (promise)
        version-number {:url "0.2.2"}
        project {:dependencies leiningen.core.project/empty-dependencies
                 :root "/tmp"
                 :profiles {:repl {:dependencies
                                   [['org.clojure/tools.nrepl version-number]]}}}]
    (with-redefs [leiningen.core.eval/eval-in-project #(deliver p %&)]
      (#'leiningen.repl/start-server project "localhost" false))
    (is (= version-number
           (first (for [dep (:dependencies (first @p))
                        :when (= 'org.clojure/tools.nrepl (first dep))]
                    (second dep)))))))
