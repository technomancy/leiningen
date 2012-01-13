(ns leiningen.jar
  "Package up all the project's files into a jar file."
  (:require [leiningen.compile :as compile]
            [leiningen.core.classpath :as classpath]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:use [leiningen.pom :only [make-pom make-pom-properties]]
        [leiningen.deps :only [deps]])
  (:import (java.util.jar Manifest JarEntry JarOutputStream)
           (java.util.regex Pattern)
           (java.util.jar JarFile)
           (java.io BufferedOutputStream FileOutputStream
                    ByteArrayInputStream)))

(declare local-repo-path) ; placate install.clj for now

;; (declare make-local-repo)

;; (defn- read-resource [resource-name]
;;   (-> (.getContextClassLoader (Thread/currentThread))
;;       (.getResourceAsStream resource-name)
;;       (slurp)))

;; (defn- read-bin-template [system]
;;   (case system
;;     :unix (read-resource "script-template")
;;     :windows (read-resource "script-template.bat")))

(defn unix-path [path]
  (.replace path "\\" "/"))

;; (defn windows-path [path]
;;   (.replace path "/" "\\"))

;; (defn local-repo-path
;;   ([group name version]
;;      (local-repo-path {:group group :name name :version version}))
;;   ([{:keys [group name version]}]
;;      (unix-path (format
;;                  "%s/%s/%s/%s/%s-%s.jar"
;;                  (.getBasedir (make-local-repo)) (.replace group "." "/")
;;                  name version name version))))

;; (defn- script-classpath-for [project deps system]
;;   (let [unix-paths (conj (for [dep deps]
;;                            (unix-path (format "$HOME/.m2/repository/%s" dep)))
;;                          (local-repo-path project))]
;;     (case system
;;       :unix (string/join ":" unix-paths)
;;       :windows (string/join ";" (for [path unix-paths]
;;                                   (windows-path
;;                                    (.replace path "$HOME"
;;                                              "%USERPROFILE%")))))))

;; (defn- shell-wrapper-name [project]
;;   (get-in project [:shell-wrapper :bin]
;;           (format "bin/%s" (:name project))))

;; (defn- shell-wrapper-contents [project bin-name main deps-fileset system]
;;   (let [file-name (case system
;;                     :unix bin-name
;;                     :windows (format "%s.bat" bin-name))
;;         bin-file (file file-name)]
;;     (format (if (.exists bin-file)
;;               (slurp bin-file)
;;               (read-bin-template system))
;;             (script-classpath-for project deps-fileset system)
;;             main (:version project))))

;; (defn- shell-wrapper-filespecs [project deps-fileset]
;;   (when (:shell-wrapper project)
;;     (let [main (or (:main (:shell-wrapper project)) (:main project))
;;           bin-name (shell-wrapper-name project)
;;           read-bin #(shell-wrapper-contents
;;                      project bin-name main deps-fileset %)]
;;       [{:type :bytes
;;         :path bin-name
;;         :bytes (.getBytes (read-bin :unix))}
;;        {:type :bytes
;;         :path (format "%s.bat" bin-name)
;;         :bytes (.getBytes (read-bin :windows))}])))

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
                    ;; (when (:shell-wrapper project)
                    ;;   {"Leiningen-shell-wrapper" (shell-wrapper-name project)})
                    (when-let [main (:main project)]
                      {"Main-Class" (.replaceAll (str main) "-" "_")})))))))

(defn manifest-map [manifest]
  (let [attrs (.getMainAttributes manifest)]
    (zipmap (map str (keys attrs)) (vals attrs))))

(defn skip-file? [file relative-path patterns]
  (or (not (.exists file))
      (.isDirectory file)
      (re-find #"^\.?#" (.getName file))
      (re-find #"~$" (.getName file))
      (some #(re-find % relative-path) patterns)))

(defmulti copy-to-jar (fn [project jar-os spec] (:type spec)))

(defn- trim-leading-str [s to-trim]
  (.replaceAll s (str "^" (Pattern/quote to-trim)) ""))

(defmethod copy-to-jar :path [project jar-os spec]
  (doseq [child (file-seq (io/file (:path spec)))]
    (let [path (trim-leading-str (unix-path (str child))
                                 (unix-path (:path spec)))]
      (when-not (skip-file? child path (:jar-exclusions project))
        (.putNextEntry jar-os (doto (JarEntry. path)
                                (.setTime (.lastModified child))))
        (io/copy child jar-os)))))

(defmethod copy-to-jar :paths [project jar-os spec]
  (doseq [path (:paths spec)]
    (copy-to-jar project jar-os {:type :path :path path})))

(defmethod copy-to-jar :bytes [project jar-os spec]
  (when-not (some #(re-find % (:path spec)) (:jar-exclusions project))
    (.putNextEntry jar-os (JarEntry. (:path spec)))
    (io/copy (ByteArrayInputStream. (:bytes spec)) jar-os)))

(defn write-jar [project out-file filespecs]
  (let [manifest (make-manifest project)]
    (with-open [jar-os (-> out-file
                           (FileOutputStream.)
                           (BufferedOutputStream.)
                           (JarOutputStream. manifest))]
      (doseq [filespec filespecs]
        (copy-to-jar project jar-os filespec)))))

(defn- filespecs [project deps-fileset]
  (concat [{:type :bytes
            :path (format "META-INF/maven/%s/%s/pom.xml"
                          (:group project) (:name project))
            ;; TODO: use pom here
            :bytes (or (make-pom project) (.getBytes ""))}
           {:type :bytes
            :path (format "META-INF/maven/%s/%s/pom.properties"
                          (:group project) (:name project))
            :bytes (make-pom-properties project)}
           {:type :path :path (str (:root project) "/project.clj")}]
          [{:type :path :path (:compile-path project)}
           {:type :paths :paths (:resources-path project)}]
          (if-not (:omit-source project)
            [{:type :paths :paths (:source-path project)}
             {:type :paths :paths (:java-source-path project)}])
          (if-let [jar-inclusions (:jar-inclusions project)]
            {:type :paths :paths jar-inclusions})
          ;; (shell-wrapper-filespecs project deps-fileset)
          ))

(defn get-jar-filename
  ([project uberjar?]
     (let [target (doto (io/file (:target-path project)) .mkdirs)
           suffix (if uberjar? "-standalone.jar" ".jar")
           jar-name (or (project (if uberjar? :uberjar-name :jar-name))
                        (str (:name project) "-" (:version project) suffix))]
       (io/file target jar-name)))
  ([project] (get-jar-filename project false)))

(defn ^{:help-arglists '([])} jar
  "Package up all the project's files into a jar file.

Create a $PROJECT-$VERSION.jar file containing project's source files as well
as .class files if applicable. If project.clj contains a :main key, the -main
function in that namespace will be used as the main-class for executable jar."
  [project]
  (let [classpath [] ;; (classpath/resolve-dependencies project)
        status (compile/compile project)]
    (if (zero? status)
      (let [jar-file (get-jar-filename project)]
        (write-jar project jar-file (filespecs project classpath))
        (println "Created" (str jar-file))
        jar-file)
      status)))
