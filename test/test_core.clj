(ns test-core
  (:use [leiningen.core] :reload-all)
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

(deftest test-matching-arity-with-project
  (is (matching-arity? "test" {} []))
  (is (matching-arity? "test" {} ["test-core"]))
  (is (not (matching-arity? "version" {} ["bogus" "arg" "s"]))))

(deftest test-matching-arity-without-project
  (is (matching-arity? "version" nil []))
  (is (not (matching-arity? "test" nil [])))
  (is (not (matching-arity? "test" nil ["test-core"]))))

(deftest test-unquote
  (let [project (binding [*ns* (find-ns 'leiningen.core)]
                  (read-project "test_projects/sample/project.clj"))]
    (is (= ['org.clojure/clojure "1.1.0"]
             (first (:dependencies project))))
    (is (= '(fn [_] (> (rand) 0.5))))))
