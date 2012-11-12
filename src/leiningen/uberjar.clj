(ns leiningen.uberjar
  "Package up the project files and dependencies into a jar file."
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.java.io :as io]
            [bultitude.core :as b]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leiningen.jar :as jar])
  (:import (java.util.zip ZipFile ZipOutputStream ZipEntry)
           (java.io File FileOutputStream PrintWriter)))

(defn read-components [zipfile]
  (if-let [entry (.getEntry zipfile "META-INF/plexus/components.xml")]
    (->> (zip/xml-zip (xml/parse (.getInputStream zipfile entry)))
         zip/children
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
      (io/copy (.getInputStream in file) out)
      (.closeEntry out)
      (.getName file))))

;; we have to keep track of every entry we've copied so that we can
;; skip duplicates.  We also collect together all the plexus components so
;; that we can merge them.
(defn- include-dep [out skip-pred [skip-set components] dep]
  (main/info "Including" (.getName dep))
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

(defn- compile-main? [{:keys [main source-paths] :as project}]
  (and main (some #(.exists (io/file % (b/path-for main))) source-paths)))

(defn- add-main [project given-main]
  (let [project (if given-main
                  (assoc project :main (symbol given-main))
                  project)]
    (if (compile-main? project)
      (update-in project [:aot] conj (:main project))
      project)))

(defn uberjar
  "Package up the project files and all dependencies into a jar file.

Includes the contents of each of the dependency jars. Suitable for standalone
distribution.

With an argument, the uberjar will be built with an alternate main.

The namespace you choose as main should have :gen-class in its ns form
as well as defining a -main function."
  ([project main]
     (let [project (add-main project main)
           project (update-in project [:jar-inclusions]
                              concat (:uberjar-inclusions project))]
       (try (jar/jar project)
            (catch Exception e
              (main/abort "Uberjar aborting because jar/compilation failed:"
                          (.getMessage e)))))
     (let [standalone-filename (jar/get-jar-filename project :uberjar)]
         (with-open [out (-> standalone-filename
                             (FileOutputStream.)
                             (ZipOutputStream.))]
           (let [project (project/unmerge-profiles project [:default])
                 deps (->> (classpath/resolve-dependencies :dependencies project)
                           (filter #(.endsWith (.getName %) ".jar")))
                 jars (cons (io/file (jar/get-jar-filename project)) deps)]
             (write-components project jars out)))
         (main/info "Created" standalone-filename)
         standalone-filename))
  ([project] (uberjar project nil)))
