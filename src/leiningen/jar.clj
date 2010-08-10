(ns leiningen.jar
  "Create a jar containing the compiled code and original source."
  (:require [leiningen.compile :as compile])
  (:use [leiningen.pom :only [make-pom make-pom-properties]]
        [clojure.contrib.io :only [to-byte-array copy file slurp*]]
        [clojure.contrib.string :only [join replace-re split]])
  (:import [java.util.jar Manifest JarEntry JarOutputStream]
           [java.util.regex Pattern]
           [java.io BufferedOutputStream FileOutputStream
            ByteArrayInputStream]))

(def bin-template (-> (.getContextClassLoader (Thread/currentThread))
                      (.getResourceAsStream "script-template")
                      (slurp*)))

(defn- classpath-path [group name version]
  (format "$HOME/.m2/repository/%s/%s/%s/%s-%s.jar"
          group name version name version))

(defn- script-classpath-for [project]
  (str (classpath-path (:group project) (:name project) (:version project)) ":"
       (join ":" (for [[dep version] (:dependencies project)
                       :let [group (or (namespace dep) (name dep))
                             group (.replaceAll group "\\." "/")]]
                   (classpath-path group (name dep) version)))))

(defn- shell-wrapper-bin [project]
  (or (:bin (:shell-wrapper project)
            (format "bin/%s" (:name project)))))

(defn- shell-wrapper-filespecs [project]
  (when (:shell-wrapper project)
    (let [main (or (:main (:shell-wrapper project)) (:main project))
          bin (shell-wrapper-bin project)]
      [{:type :bytes
        :path bin
        :bytes (.getBytes (format bin-template
                                  (script-classpath-for project)
                                  main))}])))

(def default-manifest
     {"Created-By" (str "Leiningen " (System/getProperty "leiningen.version"))
      "Built-By" (System/getProperty "user.name")
      "Build-Jdk" (System/getProperty "java.version")})

(defn make-manifest [project & [extra-entries]]
  (Manifest.
   (ByteArrayInputStream.
    (to-byte-array
     (reduce (fn [manifest [k v]]
               (str manifest "\n" k ": " v))
             "Manifest-Version: 1.0"
             (merge default-manifest (:manifest project)
                    (when (:shell-wrapper project)
                      {"Leiningen-shell-wrapper" (shell-wrapper-bin project)})
                    (when-let [main (:main project)]
                      {"Main-Class" (.replaceAll (str main) "-" "_")})))))))

(defn manifest-map [manifest]
  (let [attrs (.getMainAttributes manifest)]
    (zipmap (map str (keys attrs)) (vals attrs))))

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
     (let [jar-dir (:jar-dir project)]
       (.mkdirs (file jar-dir))
       (str jar-dir "/" jar-name)))
  ([project] (get-jar-filename project (get-default-jar-name project))))

(defn get-default-uberjar-name [project]
  (or (:uberjar-name project)
      (str (:name project) \- (:version project) "-standalone.jar")))

(defn- filespecs [project]
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
    (when (and (:resources-path project)
               (.exists (file (:resources-path project))))
      {:type :path :path (:resources-path project)})
    {:type :path :path (:compile-path project)}
    {:type :path :path (:source-path project)}
    {:type :path :path (str (:root project) "/project.clj")}]
   (shell-wrapper-filespecs project)))

(defn jar
  "Create a $PROJECT-$VERSION.jar file containing the compiled .class files as
well as the source .clj files. If project.clj contains a :main symbol, it will
be used as the main-class for an executable jar."
  ([project jar-name]
     (binding [compile/*silently* true]
       (compile/compile project))
     (let [jar-path (get-jar-filename project jar-name)]
       (write-jar project jar-path (filespecs project))
       (println "Created" jar-path)
       jar-path))
  ([project] (jar project (get-default-jar-name project))))
