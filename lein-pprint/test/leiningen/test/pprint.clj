(ns leiningen.test.pprint
  (:require [clojure.test :refer :all]
            [leiningen.pprint :refer :all]))

(def project {:foo {:bar "str"}})

(defn check [desc keys output]
  (is (= output (with-out-str (apply pprint project keys)))) desc)

(defn check-no-pretty [desc keys output]
  (check desc (concat ["--no-pretty" "--"] keys) output))

(deftest test-pprint
  (check "pretty-print a key" [":foo"] "{:bar \"str\"}\n")

  (check-no-pretty "print a key" [":foo"] "{:bar str}\n")

  (check "pretty-print a sequence" ["[:foo :bar]"] "\"str\"\n")

  (check-no-pretty "print a sequence" ["[:foo :bar]"] "str\n")

  (check "pretty-print a project" [] "{:foo {:bar \"str\"}}\n")

  (check-no-pretty "print a project" [] "{:foo {:bar str}}\n")

  (check "pretty-print multiple" [":foo" "[:foo :bar]"] "{:bar \"str\"}\n\"str\"\n")

  (check-no-pretty "print multiple" [":foo" "[:foo :bar]"] "{:bar str}\nstr\n"))
