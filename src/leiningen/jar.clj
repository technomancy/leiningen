(ns leiningen.jar
  "Package up all the project's files into a jar file."
  (:require [leiningen.pom :as pom]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.project :as project]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [bultitude.core :as b]
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
  (let [size (.length to-trim)]
    (if (.startsWith s to-trim)
      (.substring s size)
      s)))

(defn- dir-string
  "Returns the file's directory as a string, or the string representation of the
  file itself if it is a directory."
  [file]
  (if-not (. file isDirectory)
    (str (. file getParent) "/")
    (str file "/")))

(defmethod copy-to-jar :path [project jar-os acc spec]
  (when-not (acc (:path spec))
    (let [root-file (io/file (:path spec))
          root-dir-path (unix-path (dir-string root-file))]
      (doseq [child (file-seq root-file)
              :let [path (trim-leading (unix-path (str child))
                                       root-dir-path)]]
        (when-not (skip-file? child path (:jar-exclusions project))
          (.putNextEntry jar-os (doto (JarEntry. path)
                                  (.setTime (.lastModified child))))
          (io/copy child jar-os)))))
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

;; TODO: change in 3.0; this is hideous
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
  ([project classifier]
     (let [target (doto (io/file (:target-path project)) .mkdirs)
           suffix (if classifier (str "-" (name classifier) ".jar") ".jar")
           ;; TODO: splice in version to :jar-name
           name-kw (if (= classifier :standalone) :uberjar-name :jar-name)
           jar-name (or (project name-kw)
                        (str (:name project) "-" (:version project) suffix))]
       (str (io/file target jar-name))))
  ([project] (get-jar-filename project nil)))

(def whitelist-keys
  "Project keys which don't affect the production of the jar should be
propagated to the compilation phase and not stripped out."
  [:offline? :local-repo :certificates :warn-on-reflection])

(defn- compile-main? [{:keys [main source-paths] :as project}]
  (and main (some #(.exists (io/file % (b/path-for main))) source-paths)))

(defn- add-main [project given-main]
  (let [project (if given-main
                  (assoc project :main (symbol given-main))
                  project)]
    (if (and (compile-main? project) (not= :all (:aot project)))
      (update-in project [:aot] conj (:main project))
      project)))

(defn classifier-jar
  "Package up all the project's classified files into jars file.

Create a $PROJECT-$VERSION-$CLASSIFIER.jar file containing project's source
files as well as .class files if applicable. If project.clj contains a :main
key, the -main function in that namespace will be used as the main-class for
executable jar.

With an argument, the jar will be built with an alternate main."
  [{:keys [target-path] :as project} classifier spec]
  (let [spec (assoc spec
               :target-path (.getPath (io/file target-path (name classifier))))
        project (-> (project/unmerge-profiles project [:default])
                    (project/merge-profiles [spec])
                    (merge (select-keys project whitelist-keys)))]
    (eval/prep project)
    (let [jar-file (get-jar-filename project classifier)]
      (write-jar project jar-file (filespecs project []))
      (main/info "Created" (str jar-file))
      jar-file)))

(defn classifier-jars
  "Package up all the project's classified files into jars file.

Create a $PROJECT-$VERSION-$CLASSIFIER.jar file containing project's source
files as well as .class files if applicable. If project.clj contains a :main
key, the -main function in that namespace will be used as the main-class for
executable jar.

With an argument, the jar will be built with an alternate main."
  [{:keys [classifiers] :as project}]
  (reduce
   (fn [result [classifier spec]]
     (assoc result
       [:classifier (name classifier) :extension "jar"]
       (classifier-jar project classifier spec)))
   {}
   classifiers))

(defn jar
  "Package up all the project's files into a jar file.

Create a $PROJECT-$VERSION.jar file containing project's source files as well
as .class files if applicable. If project.clj contains a :main key, the -main
function in that namespace will be used as the main-class for executable jar.

With an argument, the jar will be built with an alternate main."
  ([project main]
     (let [project (-> (project/unmerge-profiles project [:default])
                       (project/merge-profiles [:provided])
                       (merge (select-keys project whitelist-keys))
                       (add-main main))]
       (eval/prep project)
       (let [jar-file (get-jar-filename project)]
         (write-jar project jar-file (filespecs project []))
         (main/info "Created" (str jar-file))
         (merge {[:extension "jar"] jar-file}
                (classifier-jars project)))))
  ([project] (jar project nil)))
