(ns leiningen.test.clean
  (:use [clojure.test]
        [clojure.java.io :only [file make-parents writer]]
        [leiningen.clean :only [clean]]
        [leiningen.test.helper :only [sample-project
                                      delete-file-recursively]]))


(def target-1 (:target-path sample-project))
(def target-2 (str (file (:root sample-project) "target-2")))
(def target-3 (str (file (:root sample-project) "target-3")))

(def target-dirs (map file [target-1 target-2 target-3]))

(defn clean-test-dirs []
  (doseq [target-dir target-dirs]
    (delete-file-recursively
     target-dir true)))

(use-fixtures :each (fn [f]
                      (doseq [target-dir target-dirs]
                        (make-parents (file target-dir "foo.tmp"))
                        (.createNewFile (file target-dir "foo.tmp")))
                      (f)
                      (clean-test-dirs)))

(deftest test-default-clean-target
  (clean sample-project)
  (is (not (.exists (file target-1)))))

(deftest test-explicit-clean-targets-with-keywords
  (let [modified-project
        (assoc sample-project
          :target-path-2 target-2
          :clean-targets [:target-path :target-path-2])]
    (clean modified-project)
    (is (not (.exists (file target-1))))
    (is (not (.exists (file target-2))))))

(deftest test-explicit-clean-targets-with-vector-of-keywords
  (testing "clean targets that are deeply nested in the project map"
   (let [modified-project
         (assoc sample-project
           :nest-1 {:nest-2 {:target-path-3 target-3}}
           :clean-targets [[:nest-1 :nest-2 :target-path-3]])]
     (clean modified-project)
     (is (not (.exists (file target-3)))))))

(deftest test-explicit-clean-targets-with-valid-string-paths
  (let [modified-project
        (assoc sample-project
          :clean-targets [target-2 target-3])]
    (clean modified-project)
    (is (not (.exists (file target-2))))
    (is (not (.exists (file target-3))))))

(deftest test-explicit-clean-targets-with-invalid-string-paths
  ;; This test could potentially be destructive, so I'm using
  ;; directory paths which do not (should not) exist.
  (testing "ancestor paths of the project root and project dirs"
    (doseq [test-dir ["../../xyz" "/xyz"
                      "xsrc" "xtest" "xresources"
                      "doc/foo"]]
      (let [modified-project
            (assoc sample-project
              :test-paths ["xtest"]
              :resource-paths ["xresources"]
              :source-paths ["xsrc"]
              :clean-targets [test-dir])]
        (is (thrown? java.io.IOException
                     (clean modified-project)))))))
