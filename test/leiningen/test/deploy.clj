(ns leiningen.test.deploy
  (:use [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.deploy]
        [leiningen.test.helper :only [delete-file-recursively
                                      tmp-dir sample-project
                                      sample-deploy-project]]))

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

(deftest ^:online test-deploy-classifier
  (testing "deployment with explicit file names uploads classifiers to repo"
    (let [deploy-dir (repo-path "deploy-classifier")
          project    (assoc sample-deploy-project
                            :deploy-repositories
                            {"snapshots" {:url (repo-url deploy-dir)}})]
      (delete-file-recursively deploy-dir :silently)
      (deploy project "snapshots"
              "deploy-me/deploy-me"
              (:version project)
              (str (:root project) "/deploy-me-0.1.0-SNAPSHOT-fat.jarr"))
      (let [dir (file deploy-dir "deploy-me/deploy-me/0.1.0-SNAPSHOT/")
            files (.list dir)]
        (is (seq (filter #(re-find #"deploy-me-0.1.0-[\d.]+-\d+-fat.jarr$" %) files)))))))

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

(deftest validate-input
  (testing "Fail if project data is missing"
    (is (thrown? clojure.lang.ExceptionInfo (deploy nil))))
  (testing "Fail if project data is missing"
    (is (thrown? clojure.lang.ExceptionInfo (deploy nil "snapshots")))))

(deftest classifiying
  (are [expected version file] (= expected (classifier version file))
      "fat" "1.2.3"          "some-project-1.2.3-fat.jar"
      "fat" "1.2.3-alpha6"   "some-project-1.2.3-alpha6-fat.jar"
      "fat" "1.2.3-SNAPSHOT" "some-project-1.2.3-SNAPSHOT-fat.jar"
      nil   "1.2.3"          "some-project-1.2.3-.jar"
      nil   "1.2.3"          "some-project-1.2.3.jar"
      nil   "0.1.0"          "/opt/workspace/mylib-0.1.0-builddir/target/mylib-0.1.0.jar"
      "RC2" "0.1.0"          "\\opt\\workspace\\mylib-0.1.0-builddir\\target\\mylib-0.1.0-RC2.jar"))
