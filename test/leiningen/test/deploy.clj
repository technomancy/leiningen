(ns leiningen.test.deploy
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.deploy]
        [leiningen.test.helper :only [delete-file-recursively
                                      tmp-dir sample-project]]))

(defn- repo-path
  [relative-repo-path]
  (format "%s/%s" tmp-dir relative-repo-path))

(defn- repo-url
  [absolute-repo-path]
  (str "file://" absolute-repo-path))

(defn- deploy-snapshots
  [project relative-repo-path & [explicit-deploy-repo?]]
  (let [repo-path (repo-path relative-repo-path)
        repo-url (repo-url repo-path)]
    (delete-file-recursively repo-path :silently)
    (deploy project (if explicit-deploy-repo?
                      repo-url
                      "snapshots"))
    (let [dir (file repo-path "nomnomnom/nomnomnom/0.5.0-SNAPSHOT/")
          files (.list dir)]
      (is (seq files))
      ;; TODO: this is vulnerable to the y3k bug!
      (is (seq (filter #(re-find #"nomnomnom-0.5.0-2\d{7}\." %) files))))))

(deftest test-deploy
  (testing "simple deployment to `snapshots` already defined in project.clj"
    (deploy-snapshots sample-project "lein-repo")))

(deftest test-deploy-custom-url
  (testing "deployment to a repo specified as a URL argument to `deploy`"
    (deploy-snapshots sample-project "lein-custom-repo" true)))

(deftest test-deploy-repositories-key
  (testing "preferring repository in :deploy-repositories over :repositories"
    (deploy-snapshots (assoc sample-project
                        :deploy-repositories
                        {"snapshots" {:url (-> "deploy-only-repo"
                                               repo-path repo-url)}})
                      "deploy-only-repo")))
