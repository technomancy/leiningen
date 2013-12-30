(ns leiningen.core.test.main
  (:use [clojure.test]
        [leiningen.core.main]))

(deftest test-matching-arity
  (is (not (matching-arity? (resolve-task "bluuugh") ["bogus" "arg" "s"])))
  (is (matching-arity? (resolve-task "bluuugh") []))
  (is (matching-arity? (resolve-task "var-args") []))
  (is (matching-arity? (resolve-task "var-args") ["test-core" "hey"]))
  (is (not (matching-arity? (resolve-task "one-or-two") [])))
  (is (matching-arity? (resolve-task "one-or-two") ["clojure"]))
  (is (matching-arity? (resolve-task "one-or-two") ["clojure" "2"]))
  (is (not (matching-arity? (resolve-task "one-or-two") ["clojure" "2" "3"]))))

(deftest test-version-satisfies
  (is (version-satisfies? "1.5.0" "1.4.2"))
  (is (not (version-satisfies? "1.4.2" "1.5.0")))
  (is (version-satisfies? "1.2.3" "1.1.1"))
  (is (version-satisfies? "1.2.0" "1.2"))
  (is (version-satisfies? "1.2" "1"))
  (is (not (version-satisfies? "1.67" "16.7"))))

(defmacro with-err-str
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(deftest arg-error-msg-test
  (are [args err-regex]
       (let [s (with-err-str
                 ;; :root true convinces resolve-and-apply that
                 ;; there's a project.clj so it doesn't throw
                 ;; that error instead
                 (try (resolve-and-apply {:root true} args)
                      (catch clojure.lang.ExceptionInfo e
                        (when-not (= {:exit-code 1} (ex-data e))
                          (throw e)))))]
         (re-find err-regex s))

       ["zero" "too" "many" "args"]
       #"(?s)Wrong number of arguments to zero task.*Expected \[\]"

       ["one-or-two"]
       #"(?s)Wrong number of arguments to one-or-two task.*Expected \[one\] or \[one two\]"))

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
