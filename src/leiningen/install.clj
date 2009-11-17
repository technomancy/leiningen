(ns leiningen.install
  (:use [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom]]
        [clojure.contrib.shell-out :only [sh with-sh-dir]])
  (:import [org.apache.maven.artifact.ant InstallTask Pom]))

(defn install [project & args]
  (let [jarfile (jar project)]
    (pom project)
    ;; TODO: use maven-ant-tasks InstallTask with in-memory Pom object
    (with-sh-dir (:root project)
      (sh "mvn" "install:install-file" "-DpomFile=pom.xml"
          (str "-Dfile=" jarfile)))
    (println "Installed" (:group project) "/" (:name project))))
