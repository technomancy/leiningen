(ns leiningen.test.deploy
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.deploy]
        [leiningen.core :only [read-project defproject]]
        [leiningen.util.file :only [delete-file-recursively tmp-dir]]
        [leiningen.test.helper :only [sample-project]]))

(deftest test-deploy
  (delete-file-recursively (format "%s/lein-repo" tmp-dir) :silently)
  (deploy sample-project "snapshots")
  (let [dir (file tmp-dir "lein-repo/nomnomnom/nomnomnom/0.5.0-SNAPSHOT/")
        files (.list dir)
        year (+ 1900 (.getYear (java.util.Date.)))]
    (is (seq files))
    ;; TODO: this is vulnerable to the y3k bug!
    (is (seq (filter #(re-find #"nomnomnom-0.5.0-2\d{7}\." %) files)))))

(deftest test-deploy-custom-url
  (delete-file-recursively (format "%s/lein-custom-repo" tmp-dir) :silently)
  (let [custom-repo (str tmp-dir "/lein-custom-repo")]
    (deploy sample-project (str "file://" custom-repo))
    (let [dir (file custom-repo "nomnomnom/nomnomnom/0.5.0-SNAPSHOT/")
          files (.list dir)
          year (+ 1900 (.getYear (java.util.Date.)))]
      (is (seq files))
      ;; TODO: this is vulnerable to the y3k bug!
      (is (seq (filter #(re-find #"nomnomnom-0.5.0-2\d{7}\." %) files))))))
