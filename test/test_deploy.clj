(ns test-deploy
  (:use [leiningen.deploy] :reload)
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.core :only [read-project defproject]]
        [leiningen.util.file :only [delete-file-recursively tmp-dir]]))

(def test-project (read-project "test_projects/sample/project.clj"))

(deftest test-deploy
  (delete-file-recursively (format "%s/lein-repo" tmp-dir) :silently)
  (deploy test-project "snapshots")
  (let [dir (file tmp-dir "lein-repo/nomnomnom/nomnomnom/0.5.0-SNAPSHOT/")
        files (.list dir)
        year (+ 1900 (.getYear (java.util.Date.)))]
    (is (seq files))
    ;; TODO: this is vulnerable to the y3k bug!
    (is (seq (filter #(re-find #"nomnomnom-0.5.0-2\d{7}\." %) files)))))
