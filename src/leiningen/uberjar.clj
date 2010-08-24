(ns leiningen.uberjar
  "Create a jar containing the compiled code, source, and all dependencies."
  (:require [clojure.xml :as xml])
  (:use [clojure.zip :only [xml-zip children]]
        [clojure.java.io :only [file copy]]
        [leiningen.clean :only [clean]]
        [leiningen.jar :only [get-jar-filename get-default-uberjar-name jar]]
        [leiningen.deps :only [deps]])
  (:import [java.util.zip ZipFile ZipOutputStream ZipEntry]
           [java.io File FileOutputStream PrintWriter]))

(defn read-components [zipfile]
  (when-let [entry (.getEntry zipfile "META-INF/plexus/components.xml")]
    (->> (xml-zip (xml/parse (.getInputStream zipfile entry)))
         children
         (filter #(= (:tag %) :components))
         first
         :content)))

(defn copy-entries
  "Copies the entries of ZipFile in to the ZipOutputStream out, skipping
  the entries which satisfy skip-pred. Returns the names of the
  entries copied."
  [in out & [skip-pred]]
  (for [file (enumeration-seq (.entries in))
        :when (not (skip-pred file))]
    (do
      (.setCompressedSize file -1) ; some jars report size incorrectly
      (.putNextEntry out file)
      (copy (.getInputStream in file) out)
      (.closeEntry out)
      (.getName file))))

;; we have to keep track of every entry we've copied so that we can
;; skip duplicates.  We also collect together all the plexus components so
;; that we can merge them.
(defn include-dep [out [skip-set components] dep]
  (println "Including" (.getName dep))
  (with-open [zipfile (ZipFile. dep)]
    [(into skip-set (copy-entries zipfile out #(skip-set (.getName %))))
     (concat components (read-components zipfile))]))

(defn- write-components [deps out]
  ;; TODO: need documentation; I have no idea what this is for. Alex!
  (let [[_ components] (reduce (partial include-dep out)
                               [#{"META-INF/plexus/components.xml"} nil]
                               deps)]
    (when-not (empty? components)
      (.putNextEntry out (ZipEntry. "META-INF/plexus/components.xml"))
      (binding [*out* (PrintWriter. out)]
        (xml/emit {:tag :component-set
                   :content
                   [{:tag :components
                     :content
                     components}]})
        (.flush *out*))
      (.closeEntry out))))

(defn uberjar
  "Create a jar like the jar task, but including the contents of each of
the dependency jars. Suitable for standalone distribution."
  ([project uberjar-name]
     (doto project
       clean deps)
     (if (jar project)
       (let [standalone-filename (get-jar-filename project uberjar-name)]
         (with-open [out (-> standalone-filename
                             (FileOutputStream.)
                             (ZipOutputStream.))]
           (let [deps (->> (.listFiles (file (:library-path project)))
                           (filter #(.endsWith (.getName %) ".jar"))
                           (cons (file (get-jar-filename project))))]
             (write-components deps out)))
         (println "Created" standalone-filename))
       (do
         (binding [*out* *err*]
           (println "Uberjar aborting because jar/compilation failed."))
         (System/exit 1))))
  ([project] (uberjar project (get-default-uberjar-name project))))
