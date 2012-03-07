(ns leiningen.install
  "Install the current project to the local repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.project :as project])
  (:use [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom]]
        [clojure.java.io :only [file copy]])
  (:import (java.util.jar JarFile)
           (java.util UUID)))

;; (defn bin-path []
;;   (doto (file (leiningen-home) "bin") .mkdirs))

;; (defn install-shell-wrappers [jarfile]
;;   (when-let [bin-name ((manifest-map (.getManifest jarfile))
;;                        "Leiningen-shell-wrapper")]
;;     (let [entry-paths (if (= :windows (get-os))
;;                         [bin-name (format "%s.bat" bin-name)]
;;                         [bin-name])]
;;       (doseq [entry-path entry-paths]
;;         (let [bin-file (file (bin-path) (last (.split entry-path "/")))]
;;           (when-let [zip-entry (.getEntry jarfile entry-path)]
;;             (.mkdirs (.getParentFile bin-file))
;;             (println "Installing shell wrapper to" (.getAbsolutePath bin-file))
;;             (copy (.getInputStream jarfile zip-entry) bin-file)
;;             (.setExecutable bin-file true)))))))


(defn install
  "Install current project to the local repository."
  ([project]
     (let [jarfile (jar project)
           pomfile (pom project)]
       (if (number? jarfile)
         ;; if we failed to create the jar, return the status code for exit
         jarfile
         (do ;; (install-shell-wrappers (JarFile. jarfile))
           (aether/install :coordinates [(symbol (:group project)
                                                 (:name project))
                                         (:version project)]
                           :jar-file (file jarfile)
                           :pom-file (file pomfile))
             0))))
  ([_ project-name version]
     (let [[name group] ((juxt name namespace) (symbol project-name))
           temp-project (format "%s/lein-%s" (System/getProperty "java.io.tmpdir") (UUID/randomUUID))
           jarfile (first (aether/resolve-dependencies
                           :coordinates [[(symbol project-name) version]]
                           :repositories (:repositories project/defaults)))]
           ;; (install-shell-wrappers (JarFile. jarfile))
       )))