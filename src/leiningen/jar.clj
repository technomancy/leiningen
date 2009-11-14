(ns leiningen.jar
  (:require [leiningen.compile :as compile])
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.duck-streams :only [copy]])
  (:import [java.util.jar JarFile JarEntry JarOutputStream]
           [java.io File FileOutputStream FileInputStream]))

;; TODO: allow setting main class (or replace with lancet)
(defn jar [project & args]
  (compile/compile project)
  (let [jar-name (str (:root project) "/" (:name project) ".jar")
        classes-dir (or (:compile-path project))]
    (with-open [jar-out (JarOutputStream. (FileOutputStream. jar-name))]
      (println "Creating jar" jar-name)
      (doseq [f (file-seq (file *compile-path*))]
        (when-not (.isDirectory f)
          (let [full-path (.getAbsolutePath f)
                relative-path (subs full-path (count *compile-path*))]
            (.putNextEntry jar-out (JarEntry. relative-path))
            (with-open [in (FileInputStream. f)]
              (copy in jar-out))
            (.closeEntry jar-out)))))))
