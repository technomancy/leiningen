(ns leiningen.jar
  "Package up all the project's files into a jar file."
  (:require [leiningen.compile :as compile]
            [clojure.string :as string]
            [lancet.core :as lancet])
  (:use [leiningen.pom :only [make-pom make-pom-properties]]
        [leiningen.deps :only [deps]]
        [clojure.java.io :only [copy file]])
  (:import (java.util.jar Manifest JarEntry JarOutputStream)
           (java.util.regex Pattern)
           (java.util.jar JarFile)
           (java.io BufferedOutputStream FileOutputStream
                    ByteArrayInputStream)))

(defn- read-resource [resource-name]
  (-> (.getContextClassLoader (Thread/currentThread))
      (.getResourceAsStream resource-name)
      (slurp)))

(defn- read-bin-template [system]
  (case system
        :unix (read-resource "script-template")
        :windows (read-resource "script-template.bat")))

(defn unix-path [path]
  (.replace path "\\" "/"))

(defn windows-path [path]
  (.replace path "/" "\\"))

(defn local-repo-path
  ([group name version]
     (local-repo-path {:group group :name name :version version}))
  ([{:keys [group name version]}]
     (unix-path (format
                 "$HOME/.m2/repository/%s/%s/%s/%s-%s.jar"
                 (.replace group "." "/") name version name version))))

(defn- script-classpath-for [project deps-fileset system]
  (let [deps (when deps-fileset
               (-> deps-fileset
                   (.getDirectoryScanner lancet/ant-project)
                   (.getIncludedFiles)))
        unix-paths (conj (for [dep deps]
                           (unix-path (format "$HOME/.m2/repository/%s" dep)))
                         (local-repo-path project))]
    (case system
          :unix (string/join ":" unix-paths)
          :windows (string/join ";" (for [path unix-paths]
                                      (windows-path
                                       (.replace path "$HOME"
                                                 "%USERPROFILE%")))))))

(defn- shell-wrapper-name [project]
  (get-in project [:shell-wrapper :bin]
          (format "bin/%s" (:name project))))

(defn- shell-wrapper-contents [project bin-name main deps-fileset system]
  (let [file-name (case system
                        :unix bin-name
                        :windows (format "%s.bat" bin-name))
        bin-file (file file-name)]
    (format (if (.exists bin-file)
              (slurp bin-file)
              (read-bin-template system))
            (script-classpath-for project deps-fileset system)
            main (:version project))))

(defn- shell-wrapper-filespecs [project deps-fileset]
  (when (:shell-wrapper project)
    (let [main (or (:main (:shell-wrapper project)) (:main project))
          bin-name (shell-wrapper-name project)
          read-bin #(shell-wrapper-contents
                     project bin-name main deps-fileset %)]
      [{:type :bytes
        :path bin-name
        :bytes (.getBytes (read-bin :unix))}
       {:type :bytes
        :path (format "%s.bat" bin-name)
        :bytes (.getBytes (read-bin :windows))}])))

(def default-manifest
     {"Created-By" (str "Leiningen " (System/getenv "LEIN_VERSION"))
      "Built-By" (System/getProperty "user.name")
      "Build-Jdk" (System/getProperty "java.version")})

(defn make-manifest [project & [extra-entries]]
  (Manifest.
   (ByteArrayInputStream.
    (.getBytes
     (reduce (fn [manifest [k v]]
               (str manifest "\n" k ": " v))
             "Manifest-Version: 1.0"
             (merge default-manifest (:manifest project)
                    (when (:shell-wrapper project)
                      {"Leiningen-shell-wrapper" (shell-wrapper-name project)})
                    (when-let [main (:main project)]
                      {"Main-Class" (.replaceAll (str main) "-" "_")})))))))

(defn manifest-map [manifest]
  (let [attrs (.getMainAttributes manifest)]
    (zipmap (map str (keys attrs)) (vals attrs))))

(defn skip-file? [file relative-path patterns]
  (or (.isDirectory file)
      (re-find #"^\.?#" (.getName file))
      (re-find #"~$" (.getName file))
      (some #(re-find % relative-path) patterns)))

(defmulti copy-to-jar (fn [project jar-os spec] (:type spec)))

(defn- trim-leading-str [s to-trim]
  (.replaceAll s (str "^" (Pattern/quote to-trim)) ""))

(defmethod copy-to-jar :path [project jar-os spec]
  (let [root (str (unix-path (:root project)) \/)
        noroot  #(trim-leading-str (unix-path %) root)
        [resources classes src]
        (map noroot (map project [:resources-path :compile-path :source-path]))]
    (doseq [child (file-seq (file (:path spec)))]
      (let [path (reduce trim-leading-str (unix-path (str child))
                         [root resources classes src "/"])]
        (when-not (skip-file? child path (:jar-exclusions project))
          (.putNextEntry jar-os (doto (JarEntry. path)
                                  (.setTime (.lastModified child))))
          (copy child jar-os))))))

(defmethod copy-to-jar :bytes [project jar-os spec]
  (.putNextEntry jar-os (JarEntry. (:path spec)))
  (copy (ByteArrayInputStream. (:bytes spec)) jar-os))

(defn write-jar [project out-filename filespecs]
  (let [manifest (make-manifest project)]
    (with-open [jar-os (-> out-filename
                           (FileOutputStream.)
                           (BufferedOutputStream.)
                           (JarOutputStream. manifest))]
      (doseq [filespec filespecs]
        (copy-to-jar project jar-os filespec)))))

(defn get-default-jar-name [project]
  (or (:jar-name project)
      (str (:name project) "-" (:version project) ".jar")))

(defn get-jar-filename
  ([project jar-name]
     (let [target-dir (:target-dir project)]
       (.mkdirs (file target-dir))
       (str target-dir "/" jar-name)))
  ([project] (get-jar-filename project (get-default-jar-name project))))

(defn get-default-uberjar-name [project]
  (or (:uberjar-name project)
      (str (:name project) \- (:version project) "-standalone.jar")))

(defn- filespecs [project deps-fileset]
  (concat
   [{:type :bytes
     :path (format "META-INF/maven/%s/%s/pom.xml"
                   (:group project)
                   (:name project))
     :bytes (make-pom project)}
    {:type :bytes
     :path (format "META-INF/maven/%s/%s/pom.properties"
                   (:group project)
                   (:name project))
     :bytes (make-pom-properties project)}
    {:type :path :path (:compile-path project)}
    {:type :path :path (str (:root project) "/project.clj")}]
   (when (and (:resources-path project)
              (.exists (file (:resources-path project))))
     [{:type :path :path (:resources-path project)}])
   (when-not (:omit-source project)
     [{:type :path :path (:source-path project)}])
   (shell-wrapper-filespecs project deps-fileset)))

(defn extract-jar
  "Unpacks jar-file into target-dir. jar-file can be a JarFile
  instance or a path to a jar file on disk."
  [jar-file target-dir]
  (let [jar (if (isa? jar-file JarFile)
              jar-file
              (JarFile. jar-file true))
        entries (enumeration-seq (.entries jar))
        target-file #(file target-dir (.getName %))]
    (doseq [entry entries :when (not (.isDirectory entry))
            :let [f (target-file entry)]]
      (.mkdirs (.getParentFile f))
      (copy (.getInputStream jar entry) f))))

(defn ^{:help-arglists '([])} jar
  "Package up all the project's files into a jar file.

Create a $PROJECT-$VERSION.jar file containing project's source files as well
as .class files if applicable. If project.clj contains a :main key, the -main
function in that namespace will be used as the main-class for executable jar."
  ([project jar-name]
     (when jar-name
       (println "WARNING: Using the jar task with an argument is deprecated."))
     (binding [compile/*silently* true]
       (when (zero? (compile/compile project))
         (let [jar-path (get-jar-filename project (get-default-jar-name project))
               deps-fileset (deps project)]
           (write-jar project jar-path (filespecs project deps-fileset))
           (println "Created" jar-path)
           jar-path))))
  ([project] (jar project nil)))
