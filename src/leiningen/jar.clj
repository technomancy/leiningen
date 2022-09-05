(ns leiningen.jar
  "Package up all the project's files into a jar file."
  (:require [leiningen.pom :as pom]
            [leiningen.clean :as clean]
            [leiningen.compile :as compile]
            [leiningen.core.project :as project]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]
            [bultitude.core :as b]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import (java.util.jar Manifest JarEntry JarOutputStream)
           (java.io BufferedOutputStream FileOutputStream
                    ByteArrayInputStream)))

(def ^:deprecated whitelist-keys
  "Deprecated: use leiningen.core.project/whitelist-keys instead"
  project/whitelist-keys)

(defn- unix-path [path]
  (.replace path "\\" "/"))

(defn- default-manifest [project]
  {"Created-By" (str "Leiningen " (main/leiningen-version))
   "Built-By" (System/getProperty "user.name")
   "Build-Jdk" (System/getProperty "java.version")
   "Leiningen-Project-ArtifactId" (:name project)
   "Leiningen-Project-GroupId" (:group project)
   "Leiningen-Project-Version" (:version project)})

(declare ^:private manifest-entry)

(defn- manifest-entries [project manifest-seq]
  (map (partial manifest-entry project) manifest-seq))

(defn- manifest-entry [project [k v]]
  (cond (symbol? v) (manifest-entry project [k (resolve v)])
        (fn? v) (manifest-entry project [k (v project)])
        (coll? v) (->> v ;; Sub-manifest = manifest section
                       (manifest-entries project)
                       (cons (str "\nName: " (name k) "\n"))
                       (string/join))
        :else (->> (str (name k) ": " v)
                   (partition-all 70)  ;; Manifest spec says lines <= 72 chars
                   (map (partial apply str))
                   (string/join "\n ")  ;; Manifest spec says join with "\n "
                   (format "%s\n"))))

(defn- place-sections-last
  "Places sections at the end of the manifest seq, as specified by the
  Manifest spec. Retains ordering otherwise (if mf is ordered)."
  [mf]
  (sort-by val (fn [v1 v2]
                 (and (not (coll? v1)) (coll? v2)))
           (seq mf)))

(defn ^:internal make-manifest [project]
  (let [project-manifest (into {} (:manifest project))
        default-manifest' (cond-> (default-manifest project)
                            ;; Add default "Main-Class" only if :main is not
                            ;; explicitly set to nil
                            (:main project :not-found)
                            (assoc "Main-Class"
                                   (munge (str (:main project 'clojure.main)))))]
    (->> (merge default-manifest' project-manifest)
         ;; manifest's "Main-Class" always overrides default "Main-Class"
         place-sections-last
         (manifest-entries project)
         (cons "Manifest-Version: 1.0\n") ;; Manifest-Version line must be first
         (string/join "")
         .getBytes
         ByteArrayInputStream.
         Manifest.)))

(defn ^:internal manifest-map [manifest]
  (let [attrs (.getMainAttributes manifest)]
    (zipmap (map str (keys attrs)) (vals attrs))))

(defn- added-file?
  "Returns true if the file is already added to the jar, false otherwise. Prints
  a warning if the file is not a directory."
  [file relative-path added-paths]
  ;; Path may be blank if it is the root path
  (if (or (string/blank? relative-path) (added-paths relative-path))
    (do
      (when-not (.isDirectory file)
        (main/info "Warning: skipped duplicate file:" relative-path))
      true)))

(defn- skip-file?
  "Skips the file if it doesn't exist. If the file is not the
  root-file (specified by :path), will also skip it if it is a dotfile, emacs
  backup file or matches an exclusion pattern."
  [file relative-path root-file exclusion-patterns inclusion-patterns]
  (or (not (.exists file))
      (and
       (not= file root-file)
       (not (some #(re-find % relative-path) inclusion-patterns))
       (or
        (re-find #"^\.?#" (.getName file))
        (re-find #"~$" (.getName file))
        (some #(re-find % relative-path) exclusion-patterns)))))

(defmulti ^:private copy-to-jar (fn [project jar-os acc spec] (:type spec)))

(defn- relativize-path
  "Relativizes a path: Removes the root-path of a path if not already removed."
  [path root-path]
  (if (.startsWith path root-path)
    (.substring path (.length root-path))
    path))

(defn- full-path ;; Q: is this a good name for this action?
  "Appends the path string with a '/' if the file is a directory."
  [file path]
  (if (.isDirectory file)
    (str path "/")
    path))

(defn- dir-string
  "Returns the file's directory as a string, or the string representation of the
  file itself if it is a directory."
  [file]
  (if-not (.isDirectory file)
    (str (.getParent file) "/")
    (str file "/")))

(defn- put-jar-entry!
  "Adds a jar entry to the Jar output stream."
  [jar-os file path]
  (.putNextEntry jar-os (doto (JarEntry. path)
                       (.setTime (.lastModified file))))
  (when-not (.isDirectory file)
    (io/copy file jar-os)))

(defmethod copy-to-jar :path [project jar-os acc spec]
  (let [root-file (io/file (:path spec))
        root-dir-path (unix-path (dir-string root-file))
        paths (for [child (file-seq root-file)
                    :let [path (relativize-path
                                 (full-path child (unix-path (str child)))
                                 root-dir-path)]]
                (when-not (or (skip-file? child path root-file
                                          (:jar-exclusions project)
                                          (:jar-inclusions project))
                              (added-file? child path acc))
                  (put-jar-entry! jar-os child path)
                  path))]
    (into acc paths)))

(defmethod copy-to-jar :paths [project jar-os acc spec]
  (reduce (partial copy-to-jar project jar-os) acc
          (for [path (:paths spec)]
            {:type :path :path path})))

(defmethod copy-to-jar :bytes [project jar-os acc spec]
  (let [path (unix-path (:path spec))]
    (when-not (some #(re-find % path) (:jar-exclusions project))
      (.putNextEntry jar-os (JarEntry. path))
      (let [bytes (if (string? (:bytes spec))
                    (.getBytes (:bytes spec))
                    (:bytes spec))]
        (io/copy (ByteArrayInputStream. bytes) jar-os)))
    (conj acc path)))

(defmethod copy-to-jar :fn [project jar-os acc spec]
  (let [f (eval (:fn spec))
        dynamic-spec (f project)]
    (copy-to-jar project jar-os acc dynamic-spec)))

(defn write-jar [project out-file filespecs]
  (with-open [jar-os (-> out-file
                         (FileOutputStream.)
                         (BufferedOutputStream.)
                         (JarOutputStream. (make-manifest project)))]
    (let [jar-paths (reduce (partial copy-to-jar project jar-os)
                            #{}
                            filespecs)]
      (if (:main project)
        (let [main-path (str (-> (string/replace (:main project) "." "/")
                                 (string/replace "-" "_"))
                              ".class")]
          (when-not (some #{main-path} jar-paths)
            (main/info "Warning: The Main-Class specified does not exist"
                       "within the jar. It may not be executable as expected."
                       "A gen-class directive may be missing in the namespace"
                       "which contains the main method, or the namespace has not"
                       "been AOT-compiled."))))
      jar-paths)))

;; TODO: change in 3.0; this is hideous
(defn- filespecs [project]
  (let [root-files (.list (io/file (:root project)))
        readmes (filter (partial re-find #"^(?i)readme") root-files)
        licenses (filter (partial re-find #"^(?i)license") root-files)
        scope (partial format "META-INF/leiningen/%s/%s/%s"
                       (:group project) (:name project))]
    (concat [{:type :bytes
              :path (format "META-INF/maven/%s/%s/pom.xml"
                            (:group project) (:name project))
              :bytes (.getBytes (pom/make-pom project))}
             {:type :bytes :path (scope "project.clj")
              :bytes (.getBytes (slurp (str (:root project) "/project.clj")))}]
            (for [doc (map (partial io/file (:root project))
                        (concat readmes licenses))
                  :when (.isFile doc)]
              {:type :bytes :path (scope (.getName doc))
               :bytes (.getBytes (slurp doc))})
            [{:type :path :path (:compile-path project)}
             {:type :paths :paths (distinct (:resource-paths project))}]
            (if-not (:omit-source project)
              [{:type :paths
                :paths (distinct (concat (:source-paths project)
                                         (:java-source-paths project)))}])
            (:filespecs project))))

;; Split out backwards-compatibility. Collapse into get-jar-filename for 3.0
(defn get-classified-jar-filename [project classifier]
  (let [target (doto (io/file (:target-path project)) utils/mkdirs)
        suffix (if classifier (str "-" (name classifier) ".jar") ".jar")
        name-kw (if (= classifier :standalone) :uberjar-name :jar-name)
        jar-name (or (project name-kw) (str (:name project) "-%s" suffix))
        jar-name (format jar-name (:version project))]
    (str (io/file target jar-name))))

(defn- compile-main? [{:keys [main source-paths] :as project}]
  (and main (not (:skip-aot (meta main)))
       (some #(or (.exists (io/file % (b/path-for main "clj")))
                  (.exists (io/file % (b/path-for main "cljc")))) source-paths)))

(def ^:private implicit-aot-warning
  (delay
   (main/info "Warning: specified :main without including it in :aot."
              "\nImplicit AOT of :main will be removed in Leiningen 3.0.0."
              "\nIf you only need AOT for your uberjar, consider adding"
              ":aot :all into your\n:uberjar profile instead.")))

(defn warn-implicit-aot [project]
  (when (and (:main project) (not (:skip-aot (meta (:main project))))
             (not= :all (:aot project))
             (not= [:all] (:aot project))
             (not (some #{(:main project)} (:aot project)))
             (not (some #(re-matches % (str (:main project)))
                        (filter compile/regex? (:aot project)))))
    (force implicit-aot-warning)))

;; TODO: remove for 3.0
(defn- add-main [project given-main]
  (warn-implicit-aot project)
  (let [project (if given-main
                  (assoc project :main (symbol given-main))
                  project)]
    (if (and (compile-main? project)
             (not= :all (:aot project))
             (not= [:all] (:aot project))
             (not (some #(= % (:main project)) (:aot project))))
      (update-in project [:aot] conj (:main project))
      project)))

(defn- process-project
  "Like update-in, but for preparing projects for (uber)jaring. f is a function
  that will take the old project and any supplied args and return the new
  project, but with whitelisted keys retained and with the main argument
  inserted if provided."
  [project main f & args]
  (-> (apply f project args)
      (project/retain-whitelisted-keys project)
      (add-main main)))

(defn- preprocess-project [project & [main]]
  (process-project project main project/unmerge-profiles
                   (project/non-leaky-profiles project)))

(defn- get-jar-filename*
  [project uberjar?]
  (get-classified-jar-filename project (if uberjar? :standalone)))

(defn get-jar-filename [project & [uberjar?]]
  (get-jar-filename* (preprocess-project project) uberjar?))

(defn build-jar
  "Build a jar for the given project and jar-file."
  [project jar-file]
  (eval/prep project)
  (write-jar project jar-file (filespecs project))
  (main/info "Created" (str jar-file))
  jar-file)

(defn main-jar
  [project provided-profiles main]
  (let [project (process-project project main project/merge-profiles
                                 provided-profiles)]
    {[:extension "jar"] (build-jar project (get-jar-filename* project nil))}))

(defn classifier-jar
  "Package up all the project's classified files into a jar file.

Create a $PROJECT-$VERSION-$CLASSIFIER.jar file containing project's source
files as well as .class files if applicable. The classifier is looked up in the
project`s :classifiers map. If it's a map, it's merged like a profile. If it's a
keyword, it's looked up in :profiles before being merged."
  [{:keys [target-path] :as project} provided-profiles classifier spec]
  (when (:dependencies spec)
    (main/warn
     "WARNING: Classifier specifies :dependencies which will be ignored."))
  (let [profiles (concat provided-profiles [::target ::classifier])
        target-profile {:target-path
                        (.getPath (io/file target-path (name classifier)))}
        project (-> project
                    (vary-meta assoc-in [:profiles ::classifier] spec)
                    (vary-meta assoc-in [:profiles ::target] target-profile)
                    (process-project nil project/merge-profiles profiles))]

    [[:classifier (name classifier) :extension "jar"]
     (build-jar project (get-classified-jar-filename project classifier))]))

(defn classifier-jars
  "Package up all the project's classified files into jar files.

Create a $PROJECT-$VERSION-$CLASSIFIER.jar file for each entry in the project's
:classifiers. Returns a map of :classifier/:extension coordinates to files."
  [{:keys [classifiers] :as project} provided-profiles]
  (into {}
        (map #(apply classifier-jar project provided-profiles %) classifiers)))

(defn jar
  "Package up all the project's files into a jar file.

Create a $PROJECT-$VERSION.jar file containing project's source files as well
as .class files if applicable. If project.clj contains a :main key, the -main
function in that namespace will be used as the main-class for executable jar.

With an argument, the jar will be built with an alternate main."
  ([project main]
     (utils/with-write-permissions (:root project)
       (when (:auto-clean project true)
         (clean/clean project))
       (let [scoped-profiles (set (project/pom-scope-profiles project :provided))
             default-profiles (set (project/expand-profile project :default))
             provided-profiles (remove
                                (set/difference default-profiles scoped-profiles)
                                (->> project meta :included-profiles
                                     (project/expand-profiles project)))
             project (preprocess-project project main)]
         (merge (main-jar project provided-profiles main)
                (classifier-jars project provided-profiles)))))
  ([project] (jar project nil)))
