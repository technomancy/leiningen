(ns leiningen.jar
  "Create a jar containing the compiled code and original source."
  (:require [leiningen.compile :as compile]
            [lancet])
  (:use [leiningen.pom :only [pom]]
        [clojure.contrib.duck-streams :only [spit]]
        [clojure.contrib.str-utils :only [str-join]]))

(defn make-manifest [project]
  (doto (str (:root project) "/classes/Manifest.txt")
    (spit (str-join "\n"
                    ["Created-By: Leiningen"
                     (str "Built-By: " (System/getProperty "user.name"))
                     (str "Build-Jdk: " (System/getProperty "java.version"))
                     (when-let [main (:main project)]
                       (str "Main-Class: " main))]))))

(defn jar
  "Create a $PROJECT.jar file containing the compiled .class files as well as
the source .clj files. If project.clj contains a :main symbol, it will be used
as the main-class for an executable jar."
  ([project jar-name]
     (compile/compile project)
     (pom project "pom-generated.xml" true)
     (let [jar-file (str (:root project) "/" jar-name)
           filesets [{:dir *compile-path*}
                     {:dir (str (:root project) "/src")}
                     ;; TODO: place in META-INF/maven/$groupId/$artifactId/pom.xml
                     ;; TODO: pom.properties
                     {:file (str (:root project) "/pom-generated.xml")}
                     {:file (str (:root project) "/project.clj")}]]
       ;; TODO: support slim, etc
       (apply lancet/jar {:jarfile jar-file
                          :manifest (make-manifest project)}
              (map lancet/fileset filesets))
       jar-file))
  ([project] (jar project (str (:name project) ".jar"))))
