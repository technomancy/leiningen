(ns leiningen.uberjar
  "Create a jar containing the compiled code, source, and all dependencies."
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.duck-streams :only [copy]]
        [leiningen.deps :only [deps]]
        [leiningen.jar :only [jar]])
  (:import [java.util.zip ZipFile]
           [java.io File FileOutputStream]))

(defn unzip
  "Unzip zipfile into the target directory."
  [zipfile target]
  (with-open [zipfile (ZipFile. zipfile)]
    (let [entries (enumeration-seq (.entries zipfile))]
      (doseq [file (remove (memfn isDirectory) entries)]
        (.mkdirs (.getParentFile (java.io.File. (str target (.getName file)))))
        (with-open [out (FileOutputStream. (str target (.getName file)))]
          (copy (.getInputStream zipfile file) out))))))

(defn uberjar
  "Create a jar like the jar task, but including the contents of each of
the dependency jars. Suitable for standalone distribution."
  [project & args]
  (doseq [dep (file-seq (file (:root project) "lib"))
          :when (.endsWith (.getName dep) ".jar")]
    (println "Unpacking" (.getName dep))
    (unzip dep *compile-path*))
  (jar project (str (:name project) "-standalone.jar")))
