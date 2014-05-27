(ns leiningen.test.deploy
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.deploy]
        [leiningen.test.helper :only [delete-file-recursively
                                      tmp-dir sample-project]]))

(defn- repo-path
  [relative-repo-path]
  (clojure.string/replace 
    (format "%s/%s" tmp-dir relative-repo-path)
    "\\" "/")) ;make path delimiters look the same / even under Windows

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

(deftest ^:online test-deploy
  (testing "simple deployment to `snapshots` already defined in project.clj"
    (deploy-snapshots sample-project "lein-repo")))

(deftest ^:online test-deploy-custom-url
  (testing "deployment to a repo specified as a URL argument to `deploy`"
    (deploy-snapshots sample-project "lein-custom-repo" true)))

(deftest ^:online test-deploy-repositories-key
  (testing "preferring repository in :deploy-repositories over :repositories"
    (deploy-snapshots (assoc sample-project
                        :deploy-repositories
                        {"snapshots" {:url (-> "deploy-only-repo"
                                               repo-path repo-url)}})
                      "deploy-only-repo")))

(deftest signing
  (testing "GPG invocation"
    (is (= (signing-args "foo.jar" nil)
           ["--yes" "-ab" "--" "foo.jar"]))
    (is (= (signing-args "foo.jar" {:gpg-key "123456"})
           ["--yes" "-ab" "--default-key" "123456" "--" "foo.jar"])))
  (testing "Key selection"
    (is (= (:gpg-key (signing-opts {:signing {:gpg-key "key-project"}}
                                   ["repo" {:signing {:gpg-key "key-repo"}}]))
           "key-repo"))
    (is (= (:gpg-key (signing-opts {:signing {:gpg-key "key-project"}}
                                   ["repo" {}]))
           "key-project")))
  (testing "Whether to sign"
    (is (= (sign-for-repo? ["foo" {:sign-releases true}]) true))
    (is (= (sign-for-repo? ["foo" {:sign-releases false}]) false))
    (is (= (sign-for-repo? ["foo" {}]) true))))

(deftest test-get-repo-aliased-repo
  (testing "Testing get-repo using aliased repo in :deploy-repositories"
    (is (= (get-repo {:repositories
                      [["derpjars" {:url "http://derp.jar/"}]
                      ["releases" {:url "http://derp.jar/snapshots"}]]
                      :deploy-repositories
                      [["releases" "derpjars"]]}
                     "releases")
           ["derpjars" {:url "http://derp.jar/"}]))))
