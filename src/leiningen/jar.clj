(ns leiningen.jar
  "Package up all the project's files into a jar file."
  (:require [leiningen.pom :as pom]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.project :as project]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import (java.util.jar Manifest JarEntry JarOutputStream)
           (java.util.regex Pattern)
           (java.util.jar JarFile)
           (java.io BufferedOutputStream FileOutputStream
                    ByteArrayInputStream)))

(defn- unix-path [path]
  (.replace path "\\" "/"))

(def ^:private default-manifest
  {"Created-By" (str "Leiningen " (main/leiningen-version))
   "Built-By" (System/getProperty "user.name")
   "Build-Jdk" (System/getProperty "java.version")})

(defn- manifest-entry [project manifest [k v]]
  (cond (symbol? v) (manifest-entry project manifest [k (resolve v)])
        (fn? v) (manifest-entry project manifest [k (v project)])
        :else (str manifest "\n" (name k) ": " v)))

(defn ^:internal make-manifest [project]
  (Manifest.
   (ByteArrayInputStream.
    (.getBytes
     (reduce (partial manifest-entry project)
             "Manifest-Version: 1.0"
             (merge default-manifest (:manifest project)
                    (if-let [main (:main project)]
                      {"Main-Class" (.replaceAll (str main) "-" "_")})))))))

(defn ^:internal manifest-map [manifest]
  (let [attrs (.getMainAttributes manifest)]
    (zipmap (map str (keys attrs)) (vals attrs))))

(defn- skip-file? [file relative-path patterns]
  (or (not (.exists file))
      (.isDirectory file)
      (re-find #"^\.?#" (.getName file))
      (re-find #"~$" (.getName file))
      (some #(re-find % relative-path) patterns)))

(defmulti ^:private copy-to-jar (fn [project jar-os acc spec] (:type spec)))

(defn- trim-leading [s to-trim]
  (.replaceAll s (str "^" (Pattern/quote to-trim)) ""))

(defmethod copy-to-jar :path [project jar-os acc spec]
  (when-not (acc (:path spec))
    (doseq [child (file-seq (io/file (:path spec)))
            :let [path (trim-leading (trim-leading (unix-path (str child))
                                                   (unix-path (:path spec)))
                                     "/")]]
      (when-not (skip-file? child path (:jar-exclusions project))
        (.putNextEntry jar-os (doto (JarEntry. path)
                                (.setTime (.lastModified child))))
        (io/copy child jar-os))))
  (conj acc (:path spec)))

(defmethod copy-to-jar :paths [project jar-os acc spec]
  (reduce (partial copy-to-jar project jar-os) acc
          (for [path (:paths spec)]
            {:type :path :path path})))

(defmethod copy-to-jar :bytes [project jar-os acc spec]
  (when-not (some #(re-find % (:path spec)) (:jar-exclusions project))
    (.putNextEntry jar-os (JarEntry. (:path spec)))
    (let [bytes (if (string? (:bytes spec))
                  (.getBytes (:bytes spec))
                  (:bytes spec))]
      (io/copy (ByteArrayInputStream. bytes) jar-os)))
  (conj acc (:path spec)))

(defmethod copy-to-jar :fn [project jar-os acc spec]
  (let [f (eval (:fn spec))
        dynamic-spec (f project)]
    (copy-to-jar project jar-os acc dynamic-spec)))

(defn write-jar [project out-file filespecs]
  (with-open [jar-os (-> out-file
                         (FileOutputStream.)
                         (BufferedOutputStream.)
                         (JarOutputStream. (make-manifest project)))]
    (reduce (partial copy-to-jar project jar-os) #{} filespecs)))

(defn- filespecs [project deps-fileset]
  (concat [{:type :bytes
            :path (format "META-INF/maven/%s/%s/pom.xml"
                          (:group project) (:name project))
            :bytes (.getBytes (pom/make-pom project))}
           {:type :bytes
            :path (format "META-INF/maven/%s/%s/pom.properties"
                          (:group project) (:name project))
            :bytes (.getBytes (pom/make-pom-properties project))}
           {:type :bytes :path (format "META-INF/leiningen/%s/%s/project.clj"
                                       (:group project) (:name project))
            :bytes (.getBytes (slurp (str (:root project) "/project.clj")))}
           {:type :bytes :path "project.clj"
            :bytes (.getBytes (slurp (str (:root project) "/project.clj")))}]
          [{:type :path :path (:compile-path project)}
           {:type :paths :paths (:resource-paths project)}]
          (if-not (:omit-source project)
            [{:type :paths :paths (:source-paths project)}
             {:type :paths :paths (:java-source-paths project)}])
          (:filespecs project)))

(defn get-jar-filename
  ([project uberjar?]
     (let [target (doto (io/file (:target-path project)) .mkdirs)
           suffix (if uberjar? "-standalone.jar" ".jar")
           ;; TODO: splice in version to :jar-name
           jar-name (or (project (if uberjar? :uberjar-name :jar-name))
                        (str (:name project) "-" (:version project) suffix))]
       (str (io/file target jar-name))))
  ([project] (get-jar-filename project false)))

(defn jar
  "Package up all the project's files into a jar file.

Create a $PROJECT-$VERSION.jar file containing project's source files as well
as .class files if applicable. If project.clj contains a :main key, the -main
function in that namespace will be used as the main-class for executable jar."
  [project]
  (let [project (-> (project/unmerge-profiles project [:default])
                    (project/merge-profiles [:provided]))]
    (eval/prep project)
    (let [jar-file (get-jar-filename project)]
      (write-jar project jar-file (filespecs project []))
      (main/info "Created" (str jar-file))
      jar-file)))
