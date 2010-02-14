(ns leiningen.compile
  "Compile the namespaces listed in project.clj or all namespaces in src."
  (:require lancet)
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
        [leiningen.deps :only [deps]])
  (:refer-clojure :exclude [compile])
  (:import org.apache.tools.ant.taskdefs.Java
           java.lang.management.ManagementFactory
           (org.apache.tools.ant.types Environment$Variable Path)))

(defn compilable-namespaces
  "Returns a seq of the namespaces that are compilable, regardless of whether
  their class files are present and up-to-date."
  [project]
  (let [nses (set (cond
                   (coll? (:namespaces project))
                   (:namespaces project)
                   (= :all (:namespaces project))
                   (find-namespaces-in-dir (file (:source-path project)))))]
    (if (:main project)
      (conj nses (:main project))
      nses)))

(defn stale-namespaces
  "Given a seq of namespaces that are both compilable and that hav missing or
  out-of-date class files."
  [project]
  (filter
   (fn [n]
     (let [ns-file (str (-> (name n)
                            (.replaceAll "\\." "/")
                            (.replaceAll "-" "_")))]
       (> (.lastModified (file (:source-path project)
                               (str ns-file ".clj")))
          (.lastModified (file (:compile-path project)
                               (str ns-file "__init.class"))))))
   (compilable-namespaces project)))

(defn find-lib-jars
  "Returns a seq of Files for all the jars in the project's library directory."
  [project]
  (filter #(.endsWith (.getName %) ".jar")
          (file-seq (file (:library-path project)))))

(defn get-by-pattern
  "Gets a value from map m, but uses the keys as regex patterns,
  trying to match against k instead of doing an exact match."
  [m k]
  (m (first (drop-while #(nil? (re-find (re-pattern %) k))
                        (keys m)))))

(def native-names
     {"Mac OS X" :macosx
      "Windows" :windows
      "Linux" :linux
      "SunOS" :solaris
      "amd64" :x86_64
      "x86_64" :x86_64
      "x86" :x86
      "i386" :x86
      "arm" :arm
      "sparc" :sparc})

(defn get-os
  "Returns a keyword naming the host OS."
  []
  (get-by-pattern native-names (System/getProperty "os.name")))

(defn get-arch
  "Returns a keyword naming the host architecture"
  []
  (get-by-pattern native-names (System/getProperty "os.arch")))

(defn find-native-lib-path
  "Returns a File representing the directory where native libs for the
  current platform are located."
  [project]
  (let [osdir (name (get-os))
        archdir (name (get-arch))
        f (file "native" osdir archdir)]
    (if (.exists f)
      f
      nil)))

(defn get-jvm-args
  "Returns a seq of strings with the arguments sent to this jvm instance."
  []
  (-> (ManagementFactory/getRuntimeMXBean)
      (.getInputArguments)
      (seq)))

(defn make-path
  "Constructs an ant Path object from Files and strings."
  [& paths]
  (let [ant-path (Path. nil)]
    (doseq [path paths]
      (.addExisting ant-path (Path. nil (str path))))
    ant-path))

(defn eval-in-project
  "Executes form in an isolated classloader with the classpath and compile path
  set correctly for the project. Pass in a handler function to have it called
  with the java task right before executing if you need to customize any of its
  properties (classpath, library-path, etc)."
  [project form & [handler]]
  (let [java (Java.)
        native-path (or (:native-path project)
                        (find-native-lib-path project))]
    (.setProject java lancet/ant-project)
    (.addSysproperty java (doto (Environment$Variable.)
                            (.setKey "clojure.compile.path")
                            (.setValue (:compile-path project))))
    (when native-path
      (.addSysproperty java (doto (Environment$Variable.)
                              (.setKey "java.library.path")
                              (.setValue (cond
                                          (= java.io.File (class native-path))
                                          (.getAbsolutePath native-path)
                                          (fn? native-path) (native-path)
                                          :default native-path)))))
    (.setClasspath java (apply make-path
                               (:source-path project)
                               (:test-path project)
                               (:compile-path project)
                               (:resources-path project)
                               (find-lib-jars project)))
    (.setFailonerror java true)
    (when (or (= :macosx (get-os)) native-path)
      (.setFork java true)
      (doseq [arg (get-jvm-args)]
        (when-not (re-matches #"^-Xbootclasspath.+" arg)
          (.setValue (.createJvmarg java) arg))))
    (.setClassname java "clojure.main")
    (.setValue (.createArg java) "-e")
    (let [cp (str (.getClasspath (.getCommandLine java)))
          form `(do (def ~'*classpath* ~cp) ~form)]
      (.setValue (.createArg java) (prn-str form)))
    ;; to allow plugins and other tasks to customize
    (when handler (handler java))
    (.execute java)))

(defn compile
  "Ahead-of-time compile the project. Looks for all namespaces under src/
  unless a list of :namespaces is provided in project.clj."
  [project]
  ;; dependencies should be resolved by explicit "lein deps",
  ;; otherwise it will be done only if :library-path is empty
  (when (empty? (find-lib-jars project))
    (deps project :skip-dev))
  (.mkdir (file (:compile-path project)))
  (if (compilable-namespaces project)
    (if-let [namespaces (seq (stale-namespaces project))]
      (eval-in-project project
                       `(doseq [namespace# '~namespaces]
                          (println "Compiling" namespace#)
                          (clojure.core/compile namespace#)))
      (println "All :namespaces already compiled."))
    (println "No :namespaces listed for compilation in project.clj.")))
