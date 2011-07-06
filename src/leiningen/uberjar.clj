(ns leiningen.uberjar
  "Package up the project files and deps into a jar file."
  (:require [clojure.xml :as xml])
  (:use [clojure.zip :only [xml-zip children]]
        [clojure.java.io :only [file copy]]
        [leiningen.core :only [abort]]
        [leiningen.clean :only [clean]]
        [leiningen.jar :only [get-jar-filename get-default-uberjar-name jar]]
        [leiningen.deps :only [deps]])
  (:import (java.util.zip ZipFile ZipOutputStream ZipEntry)
           (java.io File FileOutputStream PrintWriter)))

(defn read-components [zipfile]
  (when-let [entry (.getEntry zipfile "META-INF/plexus/components.xml")]
    (->> (xml-zip (xml/parse (.getInputStream zipfile entry)))
         children
         (filter #(= (:tag %) :components))
         first
         :content)))

;; We have to keep this separate from skip-set for performance reasons.
(defn- make-skip-pred [project]
  (fn [filename]
    (some #(re-find % filename) (:uberjar-exclusions project))))

(defn- copy-entries
  "Copies the entries of ZipFile in to the ZipOutputStream out, skipping
  the entries which satisfy skip-pred. Returns the names of the
  entries copied."
  [in out skip-set skip-pred]
  (for [file (enumeration-seq (.entries in))
        :let [filename (.getName file)]
        :when (not (or (skip-set filename) (skip-pred filename)))]
    (do
      (.setCompressedSize file -1) ; some jars report size incorrectly
      (.putNextEntry out file)
      (copy (.getInputStream in file) out)
      (.closeEntry out)
      (.getName file))))

;; we have to keep track of every entry we've copied so that we can
;; skip duplicates.  We also collect together all the plexus components so
;; that we can merge them.
(defn include-dep [out skip-pred [skip-set components] dep]
  (println "Including" (.getName dep))
  (with-open [zipfile (ZipFile. dep)]
    [(into skip-set (copy-entries zipfile out skip-set skip-pred))
     (concat components (read-components zipfile))]))

(defn write-components
  "Given a list of jarfiles, writes contents to a stream"
  [project jars out]
  (let [[_ components] (reduce (partial include-dep out
                                        (make-skip-pred project))
                               [#{"META-INF/plexus/components.xml"} nil]
                               jars)]
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
  "Package up the project files and all dependencies into a jar file.

Includes the contents of each of the dependency jars. Suitable for standalone
distribution.

With an argument, the uberjar will be built with an alternate main."
  ([project main]
     (when-not (:disable-implicit-clean project)
       (clean project))
     (if (jar (if main
                (assoc project :main (symbol main))
                project))
       (let [uberjar-name (get-default-uberjar-name project)
             standalone-filename (get-jar-filename project uberjar-name)]
         (with-open [out (-> standalone-filename
                             (FileOutputStream.)
                             (ZipOutputStream.))]
           (let [deps (->> (.listFiles (file (:library-path project)))
                           (filter #(.endsWith (.getName %) ".jar")))
                 jars (cons (file (get-jar-filename project)) deps)]
             (write-components project jars out)))
         (println "Created" standalone-filename)
         standalone-filename)
       (abort "Uberjar aborting because jar/compilation failed.")))
  ([project] (uberjar project nil)))
