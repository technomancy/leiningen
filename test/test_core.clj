(ns test-core
  (:use [leiningen.core :only [make-groups]] :reload-all)
  (:use [clojure.test]))

(deftest test-make-groups-empty-args
  (is (= [[]] (make-groups []))))

(deftest test-make-groups-single-task
  (is (= [["pom"]] (make-groups ["pom"]))))

(deftest test-make-groups-without-args
  (is (= [["clean"] ["deps"] ["test"]]
         (make-groups ["clean," "deps," "test"]))))

(deftest test-make-groups-with-args
  (is (= [["test" "test-core"] ["version"]]
         (make-groups ["test" "test-core," "version"]))))

(deftest test-make-groups-with-long-chain
  (is (= [["help" "help"] ["help" "version"] ["version"]
          ["test" "test-compile"]]
         (make-groups '("help" "help," "help" "version," "version,"
                        "test" "test-compile")))))
