(ns leiningen.test.core
  (:use [clojure.test]
        [leiningen.core]
        [leiningen.test.helper :only [sample-project]]))

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

(deftest test-matching-arity-with-project
  (is (matching-arity? "test" {} []))
  (is (matching-arity? "test" {} ["test-core"]))
  (is (not (matching-arity? "version" {} ["bogus" "arg" "s"])))
  (is (matching-arity? "search" {} ["clojure"]))
  (is (matching-arity? "search" {} ["clojure" "2"])))

(deftest test-matching-arity-without-project
  (is (matching-arity? "version" nil []))
  (is (not (matching-arity? "test" nil [])))
  (is (not (matching-arity? "test" nil ["test-core"])))
  (is (matching-arity? "search" nil ["clojure"]))
  (is (matching-arity? "search" nil ["clojure" "2"])))

(deftest test-unquote
  (is (= ['org.clojure/clojure "1.1.0"]
           (first (:dependencies sample-project))))
  (is (= '(fn [_] (> (rand) 0.5)))))

(deftest test-version-greater-eq
  (is (version-greater-eq? "1.5.0" "1.4.2"))
  (is (not (version-greater-eq? "1.4.2" "1.5.0")))
  (is (version-greater-eq? "1.2.3" "1.1.1"))
  (is (version-greater-eq? "1.2.0" "1.2"))
  (is (version-greater-eq? "1.2" "1")))
