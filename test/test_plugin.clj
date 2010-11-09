(ns test-plugin
  (:use [leiningen.plugin] :reload)
  (:use [clojure.test]))

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
  (is (= "Arguments: ([subtask project-name version])
Manage user-level plugins.

Subtasks available:
install     Download, package, and install plugin jarfile into
              ~/.lein/plugins
            Syntax: lein plugin install [GROUP/]ARTIFACT-ID VERSION
              You can use the same syntax here as when listing Leiningen
              dependencies.
uninstall   Delete the plugin jarfile
            Syntax: lein plugin uninstall [GROUP/]ARTIFACT-ID VERSION\n"
         (with-out-str (plugin "help")))))


;; TODO: figure out a clever way to actually test instaling
;; (deftest test-install
;; (install "lein-plugin" "0.1.0")
;; )
