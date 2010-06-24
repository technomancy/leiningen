(ns leiningen.jar
  "Create a jar containing the compiled code and original source."
  (:require [leiningen.compile :as compile])
  (:use [leiningen.pom :only [make-pom make-pom-properties]]
        [clojure.contrib.io :only [to-byte-array copy file]]
        [clojure.contrib.string :only [join replace-re]])
  (:import [java.util.jar Manifest JarEntry JarOutputStream]
           [java.util.regex Pattern]
           [java.io BufferedOutputStream FileOutputStream
            ByteArrayInputStream]))

(defn make-manifest [project]
  (Manifest.
   (ByteArrayInputStream.
    (to-byte-array
     (str (join "\n"
                ["Manifest-Version: 1.0" ; DO NOT REMOVE!
                 "Created-By: Leiningen"
                 (str "Built-By: " (System/getProperty "user.name"))
                 (str "Build-Jdk: " (System/getProperty "java.version"))
                 (when-let [main (:main project)]
                   (str "Main-Class: " (.replaceAll (str main) "-" "_")))])
          "\n")))))

(defn unix-path [path]
  (.replaceAll path "\\\\" "/"))

(defn skip-file? [file]
  (or (.isDirectory file)
      (re-find #"^\.?#" (.getName file))
      (re-find #"~$" (.getName file))))

(defmulti copy-to-jar (fn [project jar-os spec] (:type spec)))

(defn- trim-leading-str [s to-trim]
  (replace-re (re-pattern (str "^" (Pattern/quote to-trim))) "" s))

(defmethod copy-to-jar :path [project jar-os spec]
  (let [root (str (unix-path (:root project)) \/)
        noroot  #(trim-leading-str (unix-path %) root)
        [resources classes src]
        (map noroot (map project [:resources-path :compile-path :source-path]))]
  (doseq [child (file-seq (file (:path spec)))]
    (when-not (skip-file? child)
      (let [path (reduce trim-leading-str (unix-path (str child))
                         [root resources classes src "/"])]
        (.putNextEntry jar-os (doto (JarEntry. path)
                                (.setTime (.lastModified child))))
        (copy child jar-os))))))

(defmethod copy-to-jar :bytes [project jar-os spec]
  (.putNextEntry jar-os (JarEntry. (:path spec)))
  (copy (ByteArrayInputStream. (:bytes spec)) jar-os))

;; TODO: hacky; needed for conditional :resources-path below
(defmethod copy-to-jar nil [project jar-os spec])

(defn write-jar [project out-filename filespecs]
  (with-open [jar-os (JarOutputStream. (BufferedOutputStream.
                                        (FileOutputStream. out-filename))
                                       (make-manifest project))]
    (doseq [filespec filespecs]
      (copy-to-jar project jar-os filespec))))

(defn get-default-jar-name [project]
  (or (:jar-name project)
      (str (:name project) "-" (:version project) ".jar")))

(defn get-jar-filename [project jar-name]
  (let [jar-dir (:jar-dir project)]
    (.mkdirs (file jar-dir))
    (str jar-dir "/"  jar-name)))

(defn jar
  "Create a $PROJECT-$VERSION.jar file containing the compiled .class files as
well as the source .clj files. If project.clj contains a :main symbol, it will
be used as the main-class for an executable jar."
  ([project jar-name]
     (binding [compile/*silently* true]
       (compile/compile project))
     (let [jar-path (get-jar-filename project jar-name)
           filespecs [{:type :bytes
                       :path (format "meta-inf/maven/%s/%s/pom.xml"
                                     (:group project)
                                     (:name project))
                       :bytes (make-pom project)}
                      {:type :bytes
                       :path (format "meta-inf/maven/%s/%s/pom.properties"
                                     (:group project)
                                     (:name project))
                       :bytes (make-pom-properties project)}
                      (when (and (:resources-path project)
                                 (.exists (file (:resources-path project))))
                        {:type :path :path (:resources-path project)})
                      {:type :path :path (:compile-path project)}
                      {:type :path :path (:source-path project)}
                      {:type :path :path (str (:root project) "/project.clj")}]]
       (write-jar project jar-path filespecs)
       (println "Created" jar-path)
       jar-path))
  ([project] (jar project (get-default-jar-name project))))
