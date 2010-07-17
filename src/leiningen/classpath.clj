(ns leiningen.classpath
  (:use (clojure.contrib [io :only (file)]
                         [string :only (join)]))
  (:import org.apache.tools.ant.types.Path))

(defn find-lib-jars
  "Returns a seq of Files for all the jars in the project's library directory."
  [project]
  (filter #(.endsWith (.getName %) ".jar")
          (concat (.listFiles (file (:library-path project)))
                  ;; This must be hard-coded because it's used in
                  ;; bin/lein and thus can't be changed in project.clj.
                  (.listFiles (file (:root project) "lib/dev")))))

(defn checkout-deps-paths [project]
  (apply concat (for [dep (.listFiles (file (:root project) "checkouts"))]
                  ;; Note that this resets the leiningen.core/project var!
                  (let [proj (binding [*ns* (find-ns 'leiningen.core)]
                               (read-project (.getAbsolutePath
                                              (file dep "project.clj"))))]
                      (for [d [:source-path :compile-path :resources-path]]
                        (proj d))))))

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
          (find-lib-jars project)
          (checkout-deps-paths project)))

(defn classpath
  "Print out the classpath in which the project operates, within the
  current directory, in a format suitable for the -classpath option."
  [project]
  (println (join java.io.File/pathSeparatorChar (get-classpath project))))
