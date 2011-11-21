(ns leiningen.core.ns
  "Inspired by clojure.contrib.find-namespaces, but trimmed down to
  just what Leiningen needs."
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.util.jar JarFile)
           (java.io File BufferedReader PushbackReader InputStreamReader)))

;; The contrib version has a couple issues: it searches the whole
;; classpath rather than allowing you to specify a prefix, which means
;; you can't use it in (for example) my $HOME dir, where ~/src is over
;; 50 GB. Also it never looks past the first form to find a namespace
;; declaration.

(defn- clj? [f]
  (and (.isFile f) (.endsWith (.getName f) ".clj")))

(defn- jar? [f]
  (and (.isFile f) (.endsWith (.getName f) ".jar")))

(defn- read-ns-form
  "Given a reader on a Clojure source file, read until an ns form is found."
  [rdr]
  (let [form (try (read rdr false ::done)
                  (catch Exception e ::done))]
    (if (and (list? form) (= 'ns (first form)))
      form
      (when-not (= ::done form)
        (recur rdr)))))

(defn namespaces-in-dir
  "Return a seq of all namespaces found in Clojure source files in dir."
  [dir]
  (for [f (file-seq (io/file dir))
        :when (clj? f)
        :let [ns-form (read-ns-form (PushbackReader. (io/reader f)))]
        :when ns-form]
    (second ns-form)))

(defn- ns-in-jar-entry [jarfile entry]
  (with-open [rdr (-> jarfile
                      (.getInputStream (.getEntry jarfile (.getName entry)))
                      InputStreamReader.
                      BufferedReader.
                      PushbackReader.)]
    (read-ns-form rdr)))

(defn- namespaces-in-jar [jar]
  (let [jarfile (JarFile. jar)]
    (for [entry (enumeration-seq (.entries jarfile))
          :when (clj? entry)]
      (if-let [ns-form (ns-in-jar-entry jarfile entry)]
        (second ns-form)))))

(def ^:private classpath-files
  "A seq of all files on Leiningen's classpath."
  (for [f (.split (System/getProperty "java.class.path")
                  (System/getProperty "path.separator"))]
    (io/file f)))

(defn namespaces-matching
  "Return all namespaces matching the given prefix both on disk and
  inside jar files."
  ;; TODO: should probably accept classpath as argument
  [prefix]
  (concat (mapcat namespaces-in-dir
                  (for [dir classpath-files
                        :when (.isDirectory dir)]
                    (io/file dir (.replaceAll prefix "\\." "/"))))
          (filter #(and % (.startsWith (name %) prefix))
                  (mapcat namespaces-in-jar (filter jar? classpath-files)))))

(defn path-for
  "Transform a namespace into a .clj file path relative to classpath root."
  [namespace]
  (str (-> (str namespace)
           (.replace \- \_)
           (.replace \. \/))
       ".clj"))