(ns leiningen.core.test.main
  (:use [clojure.test]
        [leiningen.core.main]))

;; Shamelessly stolen from
;; https://github.com/clojure/clojure/blob/master/test/clojure/test_clojure/main.clj#L28
(defmacro with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)
         p# (new java.io.PrintWriter s#)]
     (binding [*err* p#]
       ~@body
       (str s#))))

(deftest test-logs-sent-to-*err*
  (testing "main/warn sent to *err*"
    (is (= "Warning message\n" (with-err-str
                                     (warn "Warning message"))))))

(deftest test-lookup-alias
  (testing "meta merging"
    (let [project {:aliases {"a" ^:foo ["root"]
                             "b" ^:bar ["a"]
                             "c" ^{:pass-through-help false :doc "Yo"} ["b"]
                             "d" ^:pass-through-help ["c"]}}]
      (are [task m] (= (meta (lookup-alias task project)) m)
           "a" {:foo true}
           "b" {:bar true}
           "c" {:doc "Yo" :pass-through-help false}
           "d" {:pass-through-help true}))))

(deftest test-task-args-help-pass-through
  (let [project {:aliases {"sirius-p" ["sirius" "partial"]
                           "s" "sirius"
                           "s-p" ["s" "partial"]
                           "sirius-pp" ["sirius-p" "foo"]
                           "sp" "s-p"
                           "test" "test"
                           "ohai" ^:pass-through-help ["run" "-m" "o.hai"]
                           "aliaso" ["ohai"]
                           "aliaso2" ["ohai"]}}]
    (testing "with :pass-through-help meta"
      (testing "on a var"
        (are [res arg] (= res (task-args arg project))
             ["help" ["sirius"]] ["help" "sirius"]
             ["sirius" ["-h"]] ["sirius" "-h"]
             ["sirius" ["-?"]] ["sirius" "-?"]
             ["sirius" ["--help"]] ["sirius" "--help"]
             ["sirius" []] ["sirius"]))
      (testing "on an alias"
        (are [res arg] (= res (task-args arg project))
             ["help" ["sirius-p"]] ["help" "sirius-p"]
             ["help" ["s"]] ["help" "s"]
             ["sirius" ["-h"]] ["s" "-h"]
             [["sirius" "partial"] ["-?"]] ["sirius-p" "-?"]
             ["sirius" ["--help"]] ["s" "--help"]
             [["sirius" "partial"] []] ["sirius-p"]
             [["sirius" "partial"] []] ["s-p"]
             [["sirius" "partial"] []] ["sp"]
             [["sirius" "partial" "foo"] ["bar"]] ["sirius-pp" "bar"]
             ["test" []] ["test"]
             ["sirius" []] ["s"]
             [["run" "-m" "o.hai"] ["-h"]] ["ohai" "-h"]
             [["run" "-m" "o.hai"] ["-h"]] ["aliaso" "-h"]
             [["run" "-m" "o.hai"] ["-h"]] ["aliaso2" "-h"]
             [["run" "-m" "o.hai"] ["--help"]] ["ohai" "--help"]
             [["run" "-m" "o.hai"] ["help"]] ["ohai" "help"])))))

(deftest test-matching-arity
  (is (not (matching-arity? (resolve-task "bluuugh") ["bogus" "arg" "s"])))
  (is (matching-arity? (resolve-task "bluuugh") []))
  (is (matching-arity? (resolve-task "var-args") []))
  (is (matching-arity? (resolve-task "var-args") ["test-core" "hey"]))
  (is (not (matching-arity? (resolve-task "one-or-two") [])))
  (is (matching-arity? (resolve-task "one-or-two") ["clojure"]))
  (is (matching-arity? (resolve-task "one-or-two") ["clojure" "2"]))
  (is (not (matching-arity? (resolve-task "one-or-two") ["clojure" "2" "3"]))))

(deftest partial-tasks
  (are [task args] (matching-arity? (resolve-task task) args)
       ["one-or-two" "clojure"] ["2"]
       ["one-or-two" "clojure" "2"] []
       ["fixed-and-var-args" "one"] ["two"]
       ["fixed-and-var-args" "one" "two"] []
       ["fixed-and-var-args" "one" "two"] ["more"])
  (are [task args] (not (matching-arity? (resolve-task task) args))
       ["one-or-two" "clojure"] ["2" "3"]
       ["one-or-two" "clojure" "2"] ["3"]
       ["fixed-and-var-args" "one"] []))

(deftest test-versions-match
  (is (versions-match? "1.2.12" "1.2.12"))
  (is (versions-match? "3.0" "3.0"))
  (is (versions-match? " 12.1.2" "12.1.2 "))
  (is (not (versions-match? "1.2" "1.3")))
  (is (not (versions-match? "1.2.0" "1.2")))
  (is (not (versions-match? "1.2" "1.2.0")))
  (is (versions-match? "2.1.3-SNAPSHOT" "2.1.3"))
  (is (versions-match? "  2.1.3-SNAPSHOT" "2.1.3"))
  (is (versions-match? "2.1.3" "2.1.3-FOO"))
  (is (not (versions-match? "3.0.0" "3.0.1-BAR"))))

(deftest test-version-satisfies
  (is (version-satisfies? "1.5.0" "1.4.2"))
  (is (not (version-satisfies? "1.4.2" "1.5.0")))
  (is (version-satisfies? "1.2.3" "1.1.1"))
  (is (version-satisfies? "1.2.0" "1.2"))
  (is (version-satisfies? "1.2" "1"))
  (is (not (version-satisfies? "1.67" "16.7"))))

(deftest one-or-two-args
  (try (binding [*err* (java.io.StringWriter.)]
         (resolve-and-apply {:root true} ["one-or-two"]))
       (catch clojure.lang.ExceptionInfo e
         (re-find #"(?s)Wrong number of arguments to one-or-two task.*Expected \[one\] or \[one two\]"
                  (.getMessage e)))))

(deftest zero-args-msg
  (try (binding [*err* (java.io.StringWriter.)]
         (resolve-and-apply {:root true} ["zero" "too" "many" "args"]))
       (catch clojure.lang.ExceptionInfo e
         (re-find #"(?s)Wrong number of arguments to zero task.*Expected \[\]"
                  (.getMessage e)))))

(def ^:private distance @#'leiningen.core.main/distance)

(deftest test-damerau-levensthein
  (is (zero? (distance "run" "run")))
  (is (zero? (distance "uberjar" "uberjar")))
  (is (zero? (distance "classpath" "classpath")))
  (is (zero? (distance "with-profile" "with-profile")))

  (is (= 1 (distance "rep" "repl")))
  (is (= 1 (distance "est" "test")))
  (is (= 1 (distance "java" "javac")))
  (is (= 1 (distance "halp" "help")))
  (is (= 1 (distance "lien" "lein")))

  (is (= 4 (distance "" "repl")))
  (is (= 6 (distance "foobar" "")))

  (is (= 2 (distance "erlp" "repl")))
  (is (= 2 (distance "deploy" "epdloy")))
  (is (= 3 (distance "pugared" "upgrade"))))

(deftest test-parse-options
  (is (= (parse-options ["--chicken"])
         [{:--chicken true} '()]))

  (is (= (parse-options ["--beef" "rare"])
         [{:--beef "rare"} []]))

  (is (= (parse-options [":fish" "salmon"])
         [{:fish "salmon"} []]))

  (is (= (parse-options ["salmon" "trout"])
         [{} ["salmon" "trout"]]))

  (is (= (parse-options ["--to-dir" "test2" "--ham"])
         [{:--ham true, :--to-dir "test2"} []]))

  (is (= (parse-options ["--to-dir" "test2" "--ham" "--" "pate"])
         [{:--ham true, :--to-dir "test2"} ["pate"]]))

  (is (= (parse-options ["--ham" "--to-dir" "test2" "pate"])
         [{:--ham true, :--to-dir "test2"} ["pate"]]))

  (is (= (parse-options ["--to-dir" "test2" "--ham" "--"])
         [{:--ham true, :--to-dir "test2"} []])))

(deftest test-spliced-project-values
  (let [p {:aliases {"go" ["echo" "write" :project/version]}
           :version "seventeen"
           :eval-in :leiningen}
        out (with-out-str (resolve-and-apply p ["go"]))]
    (is (= "write seventeen\n" out))))
