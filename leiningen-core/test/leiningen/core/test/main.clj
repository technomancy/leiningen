(ns leiningen.core.test.main
  (:use [clojure.test]
        [leiningen.core.main]))

(deftest test-group-args-empty-args
  (is (= [[]] (group-args []))))

(deftest test-group-args-single-task
  (is (= [["pom"]] (group-args ["pom"]))))

(deftest test-group-args-without-args
  (is (= [["clean"] ["deps"] ["test"]]
         (group-args ["clean," "deps," "test"]))))

(deftest test-group-args-with-args
  (is (= [["test" "test-core"] ["version"]]
         (group-args ["test" "test-core," "version"]))))

(deftest test-group-args-with-long-chain
  (is (= [["help" "help"] ["help" "version"] ["version"]
          ["test" "test-compile"]]
         (group-args '("help" "help," "help" "version," "version,"
                       "test" "test-compile")))))

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
  (is (version-satisfies? "1.2" "1")))