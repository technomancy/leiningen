(ns leiningen.install
  "Install the current project or download the project specified."
  (:use [leiningen.core :only [default-repos read-project]]
        [leiningen.jar :only [jar manifest-map local-repo-path]]
        [leiningen.deps :only [deps]]
        [leiningen.pom :only [pom]]
        [clojure.java.io :only [file copy]])
  (:import (java.util.jar JarFile)
           (java.util UUID)))

(declare container make-model make-remote-artifact
         make-remote-repo make-local-repo
         make-artifact add-metadata tmp-dir
         get-os leiningen-home)

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

;; (defn standalone-download [name group version]
;;   (.resolveAlways (.lookup container ArtifactResolver/ROLE)
;;                   (make-remote-artifact name group version)
;;                   (map make-remote-repo default-repos)
;;                   (make-local-repo)))

(defn install
  "Install current project or download specified project.

With no arguments, installs the current project and its dependencies
in your local repository. With two arguments, (group/name and version)
downloads and installs a project from a remote repository. Places
shell wrappers in ~/.lein/bin when provided."
  ([project]
     (let [jarfile (jar project)
           model (make-model project)
           artifact (make-artifact model)
           installer nil
           local-repo (make-local-repo)]
       ;; for packaging other than "pom" there should be "pom.xml"
       ;; generated and installed in local repo
       (when (not= "pom" (.getPackaging model))
         (add-metadata artifact (file (pom project))))
       (if (number? jarfile)
         ;; if we failed to create the jar, return the status code for exit
         jarfile
         (do ;; (install-shell-wrappers (JarFile. jarfile))
             (.install installer (file jarfile) artifact local-repo)
             0))))
  ([project-name version]
     (let [[name group] ((juxt name namespace) (symbol project-name))
           ;; _ (standalone-download name (or group name) version)
           temp-project (format "%s/lein-%s" tmp-dir (UUID/randomUUID))
           jarfile (local-repo-path (or group name) name version)]
       ;; (install-shell-wrappers (JarFile. jarfile))
       ;; TODO: reach in and pull out project.clj rather than
       ;; extracting it all
       ;; (try (extract-jar (file jarfile) temp-project)
       ;;      (when-let [p (read-project (str temp-project "/project.clj"))]
       ;;        (deps (dissoc p :dev-dependencies :native-dependencies)))
       ;;      (finally
       ;;       (delete-file-recursively temp-project :silently)))
       )))

;; (defn get-jar-entry [jar-file entry-name]
;;   (let [jar (JarFile. jar-file true)
;;         entry (.getJarEntry jar entry-name)]
;;     (.getInputStream jar entry)))