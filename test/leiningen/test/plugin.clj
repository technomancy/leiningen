(ns leiningen.test.plugin
  (:use [leiningen.plugin]
        [leiningen.util.file :only [unique-lein-tmp-dir
                                    delete-file-recursively]]
        [leiningen.compile :only [platform-nullsink]]
        [leiningen.core :only [read-project defproject]]
        [leiningen.test.helper :only [sample-project]]
        [clojure.test]
        [clojure.java.io :only [file]]))

(deftest test-plugin-standalone-filename
  (is (= (plugin-standalone-filename "tehgroup" "tehname" "0.0.1")
         "tehgroup-tehname-0.0.1.jar"))
  (is (= (plugin-standalone-filename nil "tehname" "0.0.1")
         "tehname-0.0.1.jar")))

(deftest test-extract-name-and-group
  (is (= (extract-name-and-group "tehgroup/tehname")
         ["tehname" "tehgroup"]))
  (is (= (extract-name-and-group "tehname")
         ["tehname" nil])))

(deftest test-help
  (is (= "Manage user-level plugins.

Subtasks available:
install     Download, package, and install plugin jarfile into
              ~/.lein/plugins
            Syntax: lein plugin install [GROUP/]ARTIFACT-ID VERSION
              You can use the same syntax here as when listing Leiningen
              dependencies.
uninstall   Delete the plugin jarfile
            Syntax: lein plugin uninstall [GROUP/]ARTIFACT-ID VERSION

Arguments: ([subtask project-name version])\n"
         (with-out-str (plugin "help")))))

(deftest test-install
  (with-out-str
    (leiningen.install/install sample-project)
    (binding [plugins-path (unique-lein-tmp-dir)
              leiningen.install/install (constantly nil)]
      (install "nomnomnom" "0.5.0-SNAPSHOT")
      (is (.exists (file plugins-path "nomnomnom-0.5.0-SNAPSHOT.jar")))
      (delete-file-recursively plugins-path))))

