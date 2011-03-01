(ns leiningen.classpath
  "Show the classpath of the current project."
  (:use [leiningen.core :only [read-project home-dir]]
        [clojure.java.io :only [file]]
        [clojure.string :only [join]])
  (:import (org.apache.tools.ant.types Path)))

(defn ^:internal find-lib-jars
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

(defn- ensure-absolute [path root]
  (.getCanonicalPath
   (let [f (file path)]
     (if (.isAbsolute f)
       f
       (file root f)))))

(defn checkout-deps-paths [project]
  (apply concat (for [dep (.listFiles (file (:root project) "checkouts"))
                      ;; Note that this resets the leiningen.core/project var!
                      :let [proj (binding [*ns* (find-ns 'leiningen.core)]
                                   (read-dependency-project dep))]
                      :when proj]
                  (for [d [:source-path :compile-path :resources-path]]
                    (ensure-absolute (proj d) dep)))))

(defn user-plugins []
  (for [jar (.listFiles (file (home-dir) "plugins"))
        :when (re-find #"\.jar$" (.getName jar))]
    (.getAbsolutePath jar)))

;; TODO: move to lancet?
(defn ^:internal make-path
  "Constructs an ant Path object from Files and strings."
  [& paths]
  (let [ant-path (Path. nil)]
    (doseq [path paths]
      (.addExisting ant-path (Path. nil (str path))))
    ant-path))

(defn get-classpath
  "Answer a list of classpath entries for PROJECT."
  [project]
  (concat (user-plugins)
          [(:test-path project)
           (:source-path project)
           (:compile-path project)
           (:dev-resources-path project)
           (:resources-path project)]
          (checkout-deps-paths project)
          (find-lib-jars project)))

(defn get-classpath-string [project]
  (join java.io.File/pathSeparatorChar (get-classpath project)))

(defn classpath
  "Print out the classpath in which the project operates in a format suitable
for java's -classpath option."
  [project]
  (println (get-classpath-string project)))
