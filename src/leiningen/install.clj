(ns leiningen.install
  "Install the project and its dependencies in your local repository."
  (:use [leiningen.core :only [home-dir default-repos]]
        [leiningen.jar :only [jar manifest-map local-repo-path]]
        [leiningen.util.maven :only [container make-model make-remote-artifact
                                     make-remote-repo make-local-repo
                                     make-artifact add-metadata]]
        [leiningen.pom :only [pom]]
        [clojure.java.io :only [file copy]])
  (:import [java.util.jar JarFile]
           [org.apache.maven.artifact.resolver ArtifactResolver]
           [org.apache.maven.artifact.installer ArtifactInstaller]))

(defn bin-path []
  (doto (file (home-dir) "bin") .mkdirs))

(defn install-shell-wrapper [jarfile]
  (when-let [bin-name ((manifest-map (.getManifest jarfile))
                       "Leiningen-shell-wrapper")]
    (let [bin-file (file (bin-path) (last (.split bin-name "/")))]
      (.mkdirs (.getParentFile bin-file))
      (println "Installing shell wrapper to" (.getAbsolutePath bin-file))
      (copy (.getInputStream jarfile (.getEntry jarfile bin-name)) bin-file)
      (.setExecutable bin-file true))))

(defn standalone-install [name group version]
  (let [resolver (.lookup container ArtifactResolver/ROLE)
        artifact (make-remote-artifact name group version)
        remote-repos (map make-remote-repo default-repos)
        local-repo (make-local-repo)]
    (.resolveAlways resolver artifact remote-repos local-repo)
    (-> (local-repo-path name group version)
        (.replace "$HOME" (System/getenv "HOME"))
        file
        JarFile.
        install-shell-wrapper)))

(defn install
  "With no arguments, installs the current project and its dependencies in
your local repository. With two arguments, downloads and installs a project
from a remote repository. May place shell wrappers in ~/.lein/bin."
  ([project]
     (let [jarfile (file (jar project))
           model (make-model project)
           artifact (make-artifact model)
           installer (.lookup container ArtifactInstaller/ROLE)
           local-repo (make-local-repo)]
       ;; for packaging other than "pom" there should be "pom.xml"
       ;; generated and installed in local repo
       (if (not= "pom" (.getPackaging model))
         (add-metadata artifact (file (pom project))))
       (install-shell-wrapper (JarFile. jarfile))
       (.install installer jarfile artifact local-repo)))
  ([project-name version]
     (let [[name group] ((juxt name namespace) (symbol project-name))]
       (standalone-install name (or group name) version))))
