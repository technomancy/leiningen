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

(def classpath-files
  (for [f (.split (System/getProperty "java.class.path")
                  (System/getProperty "path.separator"))]
    (io/file f)))

(defn clj? [f]
  (.endsWith (.getName f) ".clj"))

(defn jar? [f]
  (and (.isFile f) (.endsWith (.getName f) ".jar")))

(defn read-ns-form [r f]
  (let [form (try (read r false ::done)
                  (catch Exception e ::done))]
    (if (and (list? form) (= 'ns (first form)))
      form
      (when-not (= ::done form)
        (recur r f)))))

(defn find-ns-form [f]
  (when (and (.isFile (io/file f)) (clj? f))
    (read-ns-form (PushbackReader. (io/reader f)) f)))

(defn namespaces-in-dir [dir]
  (sort (for [f (file-seq (io/file dir))
              :let [ns-form (find-ns-form f)]
              :when ns-form]
          (second ns-form))))

(defn ns-in-jar-entry [jarfile entry]
  (with-open [rdr (-> jarfile
                      (.getInputStream (.getEntry jarfile (.getName entry)))
                      InputStreamReader.
                      BufferedReader.
                      PushbackReader.)]
    (read-ns-form rdr jarfile)))

(defn namespaces-in-jar [jar]
  (let [jarfile (JarFile. jar)]
    (for [entry (enumeration-seq (.entries jarfile))
          :when (and (not (.isDirectory entry))
                     (clj? entry))]
      (if-let [ns-form (ns-in-jar-entry jarfile entry)]
        (second ns-form)))))

(defn namespaces-matching [prefix]
  (concat (mapcat namespaces-in-dir
                  (for [dir classpath-files
                        :when (.isDirectory dir)]
                    (io/file dir (.replaceAll prefix "\\." "/"))))
          (filter #(and % (.startsWith (name %) prefix))
                  (mapcat namespaces-in-jar (filter jar? classpath-files)))))

(defn path-for [n]
  (str (-> (str n)
           (.replace \- \_)
           (.replace \. \/))
       ".clj"))