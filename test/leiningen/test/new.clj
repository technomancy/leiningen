(ns leiningen.test.new
  (:require [leiningen.new :as new]
            [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            [leiningen.test.helper :refer [delete-file-recursively abort-msg]]
            [leiningen.new :as new]
            [leiningen.core.main :as main]))

(use-fixtures :once (fn [f] (binding [main/*info* false] (f))))

(deftest test-new-with-just-project-name
  (new/new nil "test-new-proj")
  (is (= #{"README.md" "project.clj" "resources" "src" "core.clj" "test"
           "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"
           ".hgignore" "LICENSE" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "test-new-proj")))))))
  (delete-file-recursively (file "test-new-proj") :silently))

(deftest test-new-with-group-and-project-name
  (new/new nil "orgname/a-project")
  (is (= #{"src" "a_project_test.clj" "project.clj" "a_project.clj" "orgname"
           "resources" "test" ".gitignore" "README.md" "doc" "intro.md"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName)
                   (rest (file-seq (file "a-project")))))))
  (delete-file-recursively (file "a-project") :silently))

(deftest test-new-with-explicit-default-template
  (new/new nil "default" "test-new-proj")
  (is (= #{"README.md" "project.clj" "src" "core.clj" "test" "resources"
           "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "test-new-proj")))))))
  (delete-file-recursively (file "test-new-proj") :silently))

(deftest test-new-with-app-template
  (new/new nil "app" "test-new-app")
  (is (= #{"README.md" "project.clj" "src" "core.clj" "test" "resources"
           "doc" "intro.md" "test_new_app" "core_test.clj" ".gitignore"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "test-new-app")))))))
  (delete-file-recursively (file "test-new-app") :silently))

(deftest test-new-with-plugin-template
  (new/new nil "plugin" "test-new-plugin")
  (is (= #{"README.md" "project.clj" "src" "leiningen"
           "test_new_plugin.clj" ".gitignore"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "test-new-plugin")))))))
  (delete-file-recursively (file "test-new-plugin") :silently))

(deftest test-new-with-template-template
  (new/new nil "template" "test-new-template")
  (is (= #{"README.md" "project.clj" "src" "leiningen" "new" "resources"
           "test_new_template.clj" "test_new_template" "foo.clj" ".gitignore"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "test-new-template")))))))
  (delete-file-recursively (file "test-new-template") :silently))

(deftest test-new-with-nonexistent-template
  (is (re-find
       #"Could not find template for zzz"
       (with-redefs [leiningen.new/resolve-remote-template (constantly false)]
         (abort-msg new/new nil "zzz" "my-zzz")))))

(deftest test-new-with-nonexistent-template-in-mirrors
  (is (nil?
       (with-redefs
         [leiningen.core.user/profiles
          (constantly {:user
                        {:mirrors
                          {"clojars" "https://clojars.example.com"
                           "central" "https://central.exmaple.com"}}})]
         (let [name "luminus"
               sym (symbol (str "leiningen.new." name))]
           (leiningen.new/resolve-remote-template name sym))))))

(deftest ^:online test-group-id-template
  (is (fn? @(new/resolve-template "us.technomancy/liquid-cool")))
  (is (fn? @(new/resolve-template "net.ofnir/default"))))

(deftest test-new-with-*-jure-project-name
  (is (re-find
       #"names such as clojure .* are not allowed"
       (with-redefs [leiningen.new/resolve-remote-template (constantly false)]
         (abort-msg new/new nil "awesomejure")))))

(deftest test-new-with-clojure-project-name
  (is (re-find
       #"clojure.*can't be used as project name"
       (with-redefs [leiningen.new/resolve-remote-template (constantly false)]
         (abort-msg new/new nil "clojure")))))

(deftest test-new-with-show-describes-a-template
  (is (re-find
       #"^A general project template for libraries"
       (with-out-str
         (new/new nil ":show" "default"))))
  (is (re-find
       #"^A general project template for libraries"
       (with-out-str
         (new/new nil "default" ":show")))))

(deftest test-new-with-to-dir-option
  (new/new nil "test-new-proj" "--to-dir" "my-proj")
  (is (= #{"README.md" "project.clj" "src" "core.clj" "test" "resources"
           "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "my-proj")))))))
  (delete-file-recursively (file "my-proj") :silently))

(deftest test-new-with-force-option
  (.mkdir (file "test-new-proj"))
  (new/new nil "test-new-proj" "--force")
  (is (= #{"README.md" "project.clj" "src" "core.clj" "test" "resources"
           "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "test-new-proj")))))))
  (delete-file-recursively (file "test-new-proj") :silently))

(deftest test-new-with-to-dir-and-force-option
  (.mkdir (file "my-proj"))
  (new/new nil "test-new-proj" "--to-dir" "my-proj" "--force")
  (is (= #{"README.md" "project.clj" "src" "core.clj" "test" "resources"
           "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"
           "LICENSE" ".hgignore" "CHANGELOG.md"}
         (set (map (memfn getName) (rest (file-seq (file "my-proj")))))))
  (delete-file-recursively (file "my-proj") :silently))

(deftest test-new-generates-in-the-current-directory
  (let [original-pwd (System/getProperty "leiningen.original.pwd")
        new-pwd (file original-pwd "subdir") ;; TODO: make rand temp dir instead
        _ (.mkdir new-pwd)
        new-pwd (str new-pwd)]
    ;; Simulate being in a directory other than the project's top-level dir
    (System/setProperty "leiningen.original.pwd" new-pwd)

    (new/new nil "test-new-proj")
    (is (= #{"README.md" "project.clj" "src" "core.clj" "test" "resources"
             "doc" "intro.md" "test_new_proj" "core_test.clj" ".gitignore"
             "LICENSE" ".hgignore" "CHANGELOG.md"}
           (set (map (memfn getName)
                     (rest (file-seq (file new-pwd "test-new-proj")))))))
    (System/setProperty "leiningen.original.pwd" original-pwd)
    (delete-file-recursively (file new-pwd) :silently)))
