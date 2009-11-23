(ns leiningen.jar
  "Create a jar containing the compiled code and original source."
  (:require [leiningen.compile :as compile])
  (:use [leiningen.pom :only [pom]]
        [clojure.contrib.duck-streams :only [to-byte-array copy]]
        [clojure.contrib.str-utils :only [str-join re-sub]]
        [clojure.contrib.java-utils :only [file]])
  (:import [java.util.jar Manifest JarEntry JarOutputStream]
           [java.io BufferedOutputStream FileOutputStream
            ByteArrayInputStream]))

(defn make-manifest [project]
  (Manifest.
   (ByteArrayInputStream.
    (to-byte-array
     (str  (str-join "\n"
                     ["Manifest-Version: 1.0" ; DO NOT REMOVE!
                      "Created-By: Leiningen"
                      (str "Built-By: " (System/getProperty "user.name"))
                      (str "Build-Jdk: " (System/getProperty "java.version"))
                      (when-let [main (:main project)]
                        (str "Main-Class: " main))])
           "\n")))))

(defn write-file-to-jar [chop-off jar-os f]
  (if (.isDirectory f)
    (doseq [child (.listFiles f)]
      (write-file-to-jar chop-off jar-os child))
    (do
      (let [path (str f)
            path (re-sub (re-pattern (str "^" chop-off)) "" path)
            path (re-sub #"^/classes" "" path)
            path (re-sub #"^/src" "" path)
            path (re-sub #"^/" "" path)]
        (.putNextEntry jar-os (JarEntry.
                               path)))
      (copy f jar-os))))

(defn write-jar [project out-filename files]
  (with-open [jar-os (JarOutputStream. (BufferedOutputStream.
                                        (FileOutputStream. out-filename))
                                       (make-manifest project))]
    (doseq [f files]
      (write-file-to-jar (:root project) jar-os (file f)))))

(defn jar
  "Create a $PROJECT.jar file containing the compiled .class files as well as
the source .clj files. If project.clj contains a :main symbol, it will be used
as the main-class for an executable jar."
  ([project jar-name]
     (compile/compile project)
     (pom project "pom-generated.xml" true)
     (let [jar-file (str (:root project) "/" jar-name)
           files [*compile-path*
                  (str (:root project) "/src")
                  ;; TODO: place in META-INF/maven/$groupId/$artifactId/pom.xml
                  ;; TODO: pom.properties
                  (str (:root project) "/pom-generated.xml")
                  (str (:root project) "/project.clj")]]
       ;; TODO: support slim, etc
       (write-jar project jar-file files)
       jar-file))
  ([project] (jar project (str (:name project) ".jar"))))
