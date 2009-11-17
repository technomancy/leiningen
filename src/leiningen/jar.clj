(ns leiningen.jar
  (:require [leiningen.compile :as compile]
            [lancet])
  (:use [clojure.contrib.duck-streams :only [spit]]))

(defn make-manifest [project]
  (doto (str (:root project) "/Manifest.txt")
    (spit (when (:main project)
            (str "Main-Class: " (:main project) "\n")))))

(defn jar [project & args]
  (compile/compile project)
  (let [jar-file (str (:root project) "/" (:name project) ".jar")
        filesets [{:dir *compile-path*}
                  {:dir (str (:root project) "/src")}
                  {:file (str (:root project) "/project.clj")}]]
    ;; TODO: support slim, etc
    (apply lancet/jar {:jarfile jar-file
                       :manifest (make-manifest project)}
           (map lancet/fileset filesets))
    jar-file))
