(ns leiningen.classpath
  (:use [leiningen.core :only [read-project home-dir]]
        [clojure.java.io :only [file]]
        [clojure.string :only [join]])
  (:import org.apache.tools.ant.types.Path))

(defn find-lib-jars
  "Returns a seq of Files for all the jars in the project's library directory."
  [project]
  (filter #(.endsWith (.getName %) ".jar")
          (concat (.listFiles (file (:library-path project)))
                  ;; This must be hard-coded because it's used in
                  ;; bin/lein and thus can't be changed in project.clj.
                  (.listFiles (file (:root project) "lib/dev")))))

(defn- read-dependency-project [dep]
  (let [project (.getAbsolutePath (file dep "project.clj"))]
    (try (read-project project)
         (catch Exception e
           (throw (Exception. (format "Problem loading %s" project) e))))))

(defn checkout-deps-paths [project]
  (apply concat (for [dep (.listFiles (file (:root project) "checkouts"))]
                  ;; Note that this resets the leiningen.core/project var!
                  (let [proj (binding [*ns* (find-ns 'leiningen.core)]
                               (read-dependency-project dep))]
                    (for [d [:source-path :compile-path :resources-path]]
                      (proj d))))))

(defn user-plugins []
  (for [jar (.listFiles (file (home-dir) "plugins"))
        :when (re-find #"\.jar$" (.getName jar))]
    (.getAbsolutePath jar)))

(defn make-path
  "Constructs an ant Path object from Files and strings."
  [& paths]
  (let [ant-path (Path. nil)]
    (doseq [path paths]
      (.addExisting ant-path (Path. nil (str path))))
    ant-path))

(defn get-classpath
  "Answer a list of classpath entries for PROJECT."
  [project]
  (concat [(:source-path project)
           (:test-path project)
           (:compile-path project)
           (:resources-path project)
           (:test-resources-path project)]
          (user-plugins)
          (checkout-deps-paths project)
          (find-lib-jars project)))

(defn classpath
  "Print out the classpath in which the project operates, within the
  current directory, in a format suitable for the -classpath option."
  [project]
  (println (join java.io.File/pathSeparatorChar (get-classpath project))))
