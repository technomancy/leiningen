(ns leinginen.test.clean
  (:use [clojure.test]
        [clojure.java.io :only [file make-parents writer]]
        [leiningen.clean :only [clean]]
        [leiningen.test.helper :only [sample-project
                                      delete-file-recursively]]))


(def target-1 (:target-path sample-project))
(def target-2 (str (file (:root sample-project) "target-2")))
(def target-3 (str (file (:root sample-project) "target-3")))

(def target-dirs (map file [target-1 target-2 target-3]))

(use-fixtures :each (fn [f]
                      (doseq [target-dir target-dirs]
                        (delete-file-recursively
                         target-dir true)
                        (make-parents (file target-dir "foo.tmp"))
                        (.createNewFile (file target-dir "foo.tmp")))
                      (f)))

;; TODO test explicit :clean-targets with ancestor string path "../xyz","/"
;; TODO test explicit :clean-targets with src dir string paths.

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

(comment
  ;; this test may not safe to run
  ;; screw this up and you may be deleting important files on your system
 (deftest test-explicit-clean-targets-with-invalid-string-paths
   (testing "ancestor paths of the project root and project dirs"
     (doseq [ancestor ["../../xyz" "/xyz"
                       "src" "test" "doc" "resources"]]
       (let [modified-project
             (assoc sample-project
               :clean-targets [ancestor])]
         (is (thrown? java.io.IOException)
             (clean modified-project))
         (is (.exists (file ancestor))))))

   (sort (keys sample-project))


   (->> [:source-paths :java-source-paths :test-paths :resource-paths]
        (select-keys sample-project)
        vals
        flatten
        (map file)
        set)
   ))


#_(run-tests)
