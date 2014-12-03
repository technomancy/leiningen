(ns leiningen.test.clean
  (:use [clojure.test]
        [clojure.java.io :only [file make-parents writer]]
        [leiningen.clean :only [clean]]
        [leiningen.test.helper :only [sample-project]])
  (:require [leiningen.core.project :as project]))

(def target-1 (:target-path sample-project))
(def target-2 (str (file (:root sample-project) "target-2")))
(def target-3 (str (file (:root sample-project) "target-3")))

(def target-dirs (map file [target-1 target-2 target-3]))

(def delete-calls (atom '()))

(defn mock-delete-files
  "This implementation of delete-files-recursively will simply track the parameters passed in a state atom."
  [& params]
  (swap! delete-calls #(cons params %)))

(use-fixtures :each
  (fn [f]
    ;; start each test with empty state.
    (swap! delete-calls empty)

    ;; The original delete-file-recursively is potentially destructive, so let's mock it.
    (with-redefs [leiningen.clean/delete-file-recursively mock-delete-files]
      (f))))

(defn assert-cleaned
  "Asserts that the mock was called for the given target path."
  [path]
  (is (some (comp #(.startsWith path %) first) @delete-calls)
      (format "delete-files-recursively was not called for %s" path)))

(defn relative-to-absolute-project-path
  "Converts a relative path to an absolute path within the sample project"
  [path]
  (str (file (:root sample-project) path)))

(deftest test-default-clean-target
  (clean sample-project)
  (is (= target-1 (ffirst @delete-calls))))

(deftest test-explicit-clean-targets-with-keywords
  (let [modified-project
        (assoc sample-project
          :target-path-2 target-2
          :clean-targets [:target-path :target-path-2])]
    (clean modified-project)
    (assert-cleaned target-1)
    (assert-cleaned target-2)))

(deftest test-explicit-clean-targets-with-vector-of-keywords
  (testing "clean targets that are deeply nested in the project map"
   (let [modified-project
         (assoc sample-project
           :nest-1 {:nest-2 {:target-path-3 target-3}}
           :clean-targets [[:nest-1 :nest-2 :target-path-3]])]
     (clean modified-project)
     (assert-cleaned target-3))))

(deftest test-explicit-clean-targets-with-valid-string-paths
  (let [modified-project
        (assoc sample-project
          :clean-targets [target-2 target-3])]
    (clean modified-project)
    (assert-cleaned target-2)
    (assert-cleaned target-3)))

(deftest test-explicit-clean-targets-with-invalid-string-paths
  ;; These are non-existent paths outside the project root -
  ;; used in case someone tries to execute them with out the
  ;; fixture. Deleting "/" might be bad for your mental health.
  (testing "should not delete ancestor paths of the project root"
    (doseq [test-dir ["../../xyz" "/xyz"]]
      (let [modified-project
            (assoc sample-project
              :clean-targets [test-dir])]
        (is (thrown-with-msg? java.io.IOException #"project root"
                     (clean modified-project))))))

  (testing "should not delete protected project paths"
    (doseq [path-key [:test-paths :resource-paths :source-paths :java-source-paths]]
      (let [test-path (relative-to-absolute-project-path "test-path")
            modified-project
            (assoc sample-project
              path-key [test-path]
              :clean-targets [test-path])]
        (is (thrown-with-msg? java.io.IOException #"non-target"
                              (clean modified-project))))))

  (testing "should not delete project.clj"
    (let [modified-project
          (assoc sample-project
            :clean-targets [(relative-to-absolute-project-path "project.clj")])]
      (is (thrown-with-msg? java.io.IOException #"non-target"
                            (clean modified-project)))))

  (testing "should not delete docs"
    (let [modified-project
          (assoc sample-project
            :clean-targets [(relative-to-absolute-project-path "doc/stuff.doc")])]
      (is (thrown-with-msg? java.io.IOException #"non-target"
                            (clean modified-project))))))

(deftest test-protect-metadata-override
  ;; This will override the sanity check by adding :protect false to
  ;; the metadata for :clean-targets. Again, this could be destructive
  ;; so I'm using a non-existent protected directory. The result will
  ;; be that our mock delete-file-recursively will get called, and
  ;; no exceptions should be thrown.
  (testing "override protected path sanity checking"
    (doseq [test-dir
            (concat ["../../xyz" "/xyz"]
                    (map relative-to-absolute-project-path
                         ["xsrc" "xtest" "xresources"
                          "doc/foo" "project.clj"]))]
      (let [modified-project
            (assoc sample-project
              :test-paths [(relative-to-absolute-project-path "xtest")]
              :resource-paths [(relative-to-absolute-project-path "xresources")]
              :source-paths [(relative-to-absolute-project-path "xsrc")]
              :clean-targets ^{:protect false} [test-dir])]
        (clean modified-project)
        (assert-cleaned test-dir)))))

(deftest spliced-target-paths
  (let [p (-> (project/make {:root "/a/b/c" :target-path "foo/bar/%s"})
            (project/set-profiles [:dev]))]
    (is (= "/a/b/c/foo/bar/dev" (:target-path p)))
    (clean p)
    (assert-cleaned "/a/b/c/foo/bar/dev")))
