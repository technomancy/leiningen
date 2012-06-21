(ns leiningen.plugin
  "Manage user-level plugins."
  (:use [leiningen.core :only [read-project abort]]
        [leiningen.uberjar :only [write-components]]
        [leiningen.deps :only [deps dev-exclusions]]
        [leiningen.jar :only [local-repo-path extract-jar
                              get-default-uberjar-name]]
        [leiningen.util.file :only [tmp-dir delete-file-recursively]]
        [leiningen.util.paths :only [leiningen-home get-os]]
        [clojure.java.io :only [file copy]])
  (:require [leiningen.install]
            [leiningen.help])
  (:import (java.util.zip ZipOutputStream)
           (java.io File FileOutputStream)))

(def plugins-path (file (leiningen-home) "plugins"))

(defn plugin-standalone-filename [group name version]
  (str (if group (str group "-") nil) name "-" version ".jar"))

(defn extract-name-and-group [project-name]
  ((juxt name namespace) (symbol project-name)))

(defn uninstall
  "Delete the plugin jarfile
Syntax: lein plugin uninstall [GROUP/]ARTIFACT-ID VERSION"
  [project-name version]
  (let [[name group] (extract-name-and-group project-name)
        jarfile (file plugins-path
                      (plugin-standalone-filename group name version))]
    (if (.exists jarfile)
      (if (.delete jarfile)
        (println (format "Uninstalled %s %s." project-name version))
        (abort (format "Failed to delete \"%s\"." (.getAbsolutePath jarfile))))
      (abort (format "Plugin \"%s %s\" doesn't appear to be installed."
                     project-name version)))))

(defn- uninstall-old [project-name]
  (doseq [plugin (.list plugins-path)
          :let [pat (re-pattern (format "^\\Q%s\\E-.*\\.jar$" project-name))]
          :when (re-find pat plugin)]
    (.delete (file plugins-path plugin))))

(defn locate-project-file
  [temp-project group artifact]
  (when-not (.exists (file temp-project "project.clj"))
    (copy
     (file temp-project "META-INF/leiningen" group artifact "project.clj")
     (file temp-project "project.clj"))))

(defn install
  "Download, package, and install plugin jarfile into ~/.lein/plugins
Syntax: lein plugin install [GROUP/]ARTIFACT-ID VERSION
  You can use the same syntax here as when listing Leiningen
  dependencies."
  [project-name version]
  (uninstall-old project-name)
  (leiningen.install/install project-name version)
  (.mkdirs plugins-path)
  (let [[name group] (extract-name-and-group project-name)
        temp-project (file tmp-dir (str "lein-" (java.util.UUID/randomUUID)))
        jarfile (-> (local-repo-path (or group name) name version)
                    (.replace "$HOME" (System/getProperty "user.home")))
        _ (extract-jar (file jarfile) temp-project)
        _ (locate-project-file temp-project (or group name) name)
        project (read-project (str (file temp-project "project.clj")))
        project (assoc project :exclusions dev-exclusions)
        standalone-filename (plugin-standalone-filename group name version)]
    (deps (dissoc project :dev-dependencies :native-dependencies
                  :eval-in-leiningen))
    (with-open [out (-> (file plugins-path standalone-filename)
                        (FileOutputStream.)
                        (ZipOutputStream.))]
      (let [deps (->> (.listFiles (file (:library-path project)))
                      (filter #(.endsWith (.getName %) ".jar"))
                      (cons (file jarfile)))]
        (write-components project deps out)))
    ;; Ignore exceptions on Windows; see #252.
    (delete-file-recursively temp-project (= :windows (get-os)))
    (println "Created" standalone-filename)))

(defn ^{:doc "Manage user-level plugins."
        :help-arglists '([subtask project-name version])
        :subtasks [#'install #'uninstall]}
  plugin
  ([] (println (leiningen.help/help-for "plugin")))
  ([_] (plugin))
  ([_ _] (plugin))
  ([subtask project-name version]
    (case subtask
      "install" (install project-name version)
      "uninstall" (uninstall project-name version)
      (plugin))))
