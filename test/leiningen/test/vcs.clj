(ns leiningen.test.vcs
  (:require [clojure.test :refer :all]
            [leiningen.vcs :as vcs]))

(deftest parsed-args
  (testing "VCS tag argument parsing"
    (are [args parsed-args] (= (vcs/parse-tag-args args) parsed-args)
      [] {:sign? true}
      ["v"] {:prefix "v" :sign? true}
      ["v" "--sign"] {:prefix "v" :sign? true}
      ["--sign"] {:sign? true}
      ["--no-sign"] {:sign? false}
      ["--no-sign" "v"] {:prefix "v" :sign? false}
      ["-s"] {:sign? true}
      ["v" "r"] {:prefix "r" :sign? true})))
