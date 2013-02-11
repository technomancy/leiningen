(ns leiningen.install
  "Install the current project to the local repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.project :as project]
            [leiningen.jar :as jar]
            [leiningen.pom :as pom]
            [clojure.java.io :as io])
  (:import (java.util.jar JarFile)
           (java.util UUID)))

(defn install
  "Install current project to the local repository."
  [project]
  (let [jarfiles (jar/jar project)
        pomfile (pom/pom project)
        local-repo (:local-repo project)]
    (aether/install
     :coordinates [(symbol (:group project) (:name project))
                   (:version project)]
     :artifact-map jarfiles
     :pom-file (io/file pomfile)
     :local-repo local-repo)))
