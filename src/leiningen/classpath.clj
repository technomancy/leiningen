(ns leiningen.classpath
  "Print the classpath of the current project."
  (:use [leiningen.core :only [read-project no-dev?]]
        [leiningen.deps :only [find-jars]]
        [leiningen.util.paths :only [leiningen-home]]
        [clojure.java.io :only [file]]
        [clojure.string :only [join]])
  (:import (org.apache.tools.ant.types Path)))

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
                      :let [proj (read-dependency-project dep)]
                      :when proj]
                  (for [d (:checkout-deps-shares project [:source-path
                                                          :compile-path
                                                          :resources-path])]
                    (ensure-absolute (d proj) dep)))))

(defn user-plugins []
  (for [jar (.listFiles (file (leiningen-home) "plugins"))
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
  (concat (if-not (no-dev?)
            [(:test-path project)
             (:dev-resources-path project)])
          [(:source-path project)
           (:compile-path project)
           (:resources-path project)]
          (:extra-classpath-dirs project)
          (checkout-deps-paths project)
          (find-jars project)
          (if-not (no-dev?)
            (user-plugins))))

(defn get-classpath-string [project]
  (join java.io.File/pathSeparatorChar (get-classpath project)))

(defn classpath
  "Print the classpath of the current project.

Suitable for java's -classpath option.

Warning: due to a bug in ant, calling this task with :local-repo-classpath set
when the dependencies have not been fetched will result in spurious output before
the classpath. In such cases, pipe to tail -n 1."
  [project]
  (println (get-classpath-string project)))
