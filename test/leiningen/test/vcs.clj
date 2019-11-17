(ns leiningen.test.vcs
  (:require [clojure.test :refer :all]
            [leiningen.vcs :as vcs]))

(deftest parsed-args
  (testing "VCS tag argument parsing"
    (are [args parsed-args] (= (vcs/parse-tag-args args) parsed-args)
      [] {:sign? true :annotate? true}
      ["v"] {:prefix "v" :sign? true :annotate? true}
      ["v" "--sign"] {:prefix "v" :sign? true :annotate? true}
      ["--sign"] {:sign? true :annotate? true}
      ["--no-sign"] {:sign? false :annotate? true}
      ["--no-sign" "v"] {:prefix "v" :sign? false :annotate? true}
      ["--no-annotate"] {:sign? true :annotate? false}
      ["--annotate"] {:sign? true :annotate? true}
      ["--no-sign" "--no-annotate" "v"] {:sign? false :annotate? false :prefix "v"}
      ["-s"] {:sign? true :annotate? true}
      ["v" "r"] {:prefix "r" :sign? true :annotate? true})))
