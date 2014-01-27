(ns leiningen.test.do
  (:refer-clojure :exclude [do])
  (:use [clojure.test]
        [leiningen.do]))

(deftest test-group-args-empty-args
  (is (= [] (group-args []))))

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

(deftest test-group-existing-collections
  (is (= [["clean"] ["test" ":integration"] '("deploy" "clojars")]
         (group-args ["clean" ["test" ":integration"]
                      '("deploy" "clojars")])))
  (is (= [["foo" "bar"] ["baz" "quux"]]
         (group-args [["foo" "bar"] ["baz" "quux"]])))
  (is (= [["foo" "bar"] ["baz"]]
         (group-args [["foo" "bar"] "baz"])))
  (is (= [["combinations"] ["work"] ["as" "well"]]
         (group-args ["combinations," "work" ["as" "well"]]))))
