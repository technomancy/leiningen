(ns leiningen.test.helper
  (:require [leiningen.core.project :as project]
            [leiningen.core.user :as user]
            [clojure.java.io :as io]))

;; TODO: fix
(def local-repo (io/file (System/getProperty "user.home") ".m2" "repository"))

(def tmp-dir (System/getProperty "java.io.tmpdir"))

(defn m2-dir [n v]
  (io/file local-repo
           (if (string? n) n (or (namespace n) (name n))) (name n) v))

(defn- read-test-project [name]
  (with-redefs [user/profiles (constantly {})]
    (project/init-project
     (project/read (format "test_projects/%s/project.clj" name)))))

(def sample-project (read-test-project "sample"))

(def sample-failing-project (read-test-project "sample_failing"))

(def sample-no-aot-project (read-test-project "sample_no_aot"))

(def tricky-name-project (read-test-project "tricky-name"))

(def native-project (read-test-project "native"))

(def overlapped-sourcepaths-project (read-test-project "overlapped-sourcepaths"))

;; grumble, grumble; why didn't this make it into clojure.java.io?
(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (System/gc) ; This sometimes helps release files for deletion on windows.
  (let [f (io/file f)]
    (if (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (io/delete-file f silently)))
