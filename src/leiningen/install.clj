(ns leiningen.install
  "Install the project in your local repository. Currently requires Maven."
  (:use [leiningen.jar :only [jar]]
        [leiningen.pom :only [pom]]
        [clojure.contrib.shell-out :only [sh with-sh-dir]])
  (:import [org.apache.maven.artifact.ant InstallTask Pom]))

(defn install
  "Install the project and its dependencies into ~/.m2/repository using Maven."
  [project & args]
  (let [jarfile (jar project)]
    (pom project "pom-generated.xml" true)
    ;; TODO: use maven-ant-tasks InstallTask with in-memory Pom object
    (with-sh-dir (:root project)
      (try (sh "mvn" "install:install-file" "-DpomFile=pom-generated.xml"
               (str "-Dfile=" jarfile))
           (println "Installed" (:group project) "/" (:name project))
           (catch java.io.IOException _
             (.write *err* "Currently maven must be present to install.\n")
             (System/exit 1))))))
