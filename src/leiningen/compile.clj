(ns leiningen.compile
  "Compile Clojure source into .class files."
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [bultitude.core :as b]
            [clojure.java.io :as io])
  (:refer-clojure :exclude [compile])
  (:import (java.io PushbackReader)))

(defn- regex? [str-or-re]
  (instance? java.util.regex.Pattern str-or-re))

(defn- matching-nses [re-or-sym namespaces]
  (if (regex? re-or-sym)
    (filter #(re-find re-or-sym (name %)) namespaces)
    [re-or-sym]))

(defn- find-namespaces-by-regex [project nses]
  (let [avail-nses (->> (:source-paths project)
                     (map io/file)
                     (b/namespaces-on-classpath :classpath))]
   (mapcat #(matching-nses % avail-nses) nses)))

(defn compilable-namespaces
  "Returns a seq of the namespaces that are compilable, regardless of whether
  their class files are present and up-to-date."
  [{:keys [aot source-paths] :as project}]
  (if (= :all aot)
    (b/namespaces-on-classpath :classpath (map io/file source-paths))
    (find-namespaces-by-regex project aot)))

(defn stale-namespaces
  "Return a seq of namespaces that are both compilable and that have missing or
  out-of-date class files."
  [project]
  (for [namespace (compilable-namespaces project)
        :let [rel-source (b/path-for namespace)
              source (first (for [source-path (:source-paths project)
                                  :let [file (io/file source-path rel-source)]
                                  :when (.exists file)]
                              file))]
        :when source
        :let [rel-compiled (.replaceFirst rel-source "\\.clj$" "__init.class")
              compiled (io/file (:compile-path project) rel-compiled)]
        :when (>= (.lastModified source) (.lastModified compiled))]
    namespace))

 ;; .class file cleanup

(defn- package-in-project?
  "Tests if the package found in the compile path exists as a directory in the source path."
  [found-path compile-path source-path]
  (.isDirectory (io/file (.replace found-path compile-path source-path))))

(defn- has-source-package?
  "Test if the class file's package exists as a directory in source-paths."
  [project f source-paths]
  (and source-paths
       (let [[[parent] [_ _ proxy-mod-parent]]
             (->> f, (iterate #(.getParentFile %)),
                  (take-while identity), rest,
                  (split-with #(not (re-find #"^proxy\$" (.getName %)))))
             found-path (.getPath (or proxy-mod-parent parent))
             compile-path (:compile-path project)]
         (some #(package-in-project? found-path compile-path %) source-paths))))

(defn- source-in-project?
  "Tests if a file found in the compile path exists in the source path."
  [parent compile-path source-path]
  (.exists (io/file (str (.replace parent compile-path source-path) ".clj"))))

(defn- class-in-project? [project f]
  (or (has-source-package? project f (:source-paths project))
      (has-source-package? project f (:java-source-paths project))
      (let [parent (.getParent f)
            compile-path (:compile-path project)]
        (some #(source-in-project? parent compile-path %) (:source-paths project)))))

(defn- relative-path [project f]
  (let [root-length (if (= \/ (last (:compile-path project)))
                      (count (:compile-path project))
                      (inc (count (:compile-path project))))]
    (subs (.getAbsolutePath f) root-length)))

(defn- blacklisted-class? [project f]
  ;; true indicates all non-project classes are blacklisted
  (or (true? (:clean-non-project-classes project))
      (some #(re-find % (relative-path project f))
            (:clean-non-project-classes project))))

(defn- whitelisted-class? [project f]
  (or (class-in-project? project f)
      (and (:class-file-whitelist project)
           (re-find (:class-file-whitelist project)
                    (relative-path project f)))))

(defn clean-non-project-classes [project]
  (when (:clean-non-project-classes project)
    (doseq [f (file-seq (io/file (:compile-path project)))
            :when (and (.isFile f)
                       (not (whitelisted-class? project f))
                       (blacklisted-class? project f))]
      (.delete f))))

(defn compile
  "Compile Clojure source into .class files.

Uses the namespaces specified under :aot in project.clj or those given
as command-line arguments. Use :all argument to compile everything.

This should automatically happen when required if it's configured correctly; it
shouldn't need to be manually invoked. See the javac task as well.

Compiling code loads the namespace, so keep side-effects out of the top level.
Code that should run on startup belongs in a -main defn."
  ([project]
     (if-let [namespaces (seq (stale-namespaces project))]
       (let [form `(doseq [namespace# '~namespaces]
                     (println "Compiling" namespace#)
                     (clojure.core/compile namespace#))
             project (update-in project [:prep-tasks]
                                (partial remove #{"compile"}))]
         (try (eval/eval-in-project project form)
              (catch Exception e
                (main/abort "Compilation failed:" (.getMessage e)))
              (finally (clean-non-project-classes project))))
       (main/debug "All namespaces already AOT compiled.")))
  ([project & namespaces]
     (compile (assoc project :aot (if (= namespaces [":all"])
                                    :all
                                    (map symbol namespaces))))))
