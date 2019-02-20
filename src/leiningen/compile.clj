(ns leiningen.compile
  "Compile Clojure source into .class files."
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [bultitude.core :as b]
            [clojure.java.io :as io])
  (:refer-clojure :exclude [compile])
  (:import (java.io PushbackReader File)))

(defn regex? [str-or-re]
  (instance? java.util.regex.Pattern str-or-re))

(defn- matching-nses [re-or-sym namespaces]
  (if (regex? re-or-sym)
    (filter #(re-find re-or-sym (name %)) namespaces)
    [re-or-sym]))

(defn- find-namespaces-by-regex [project nses]
  (let [avail-nses (->> (:source-paths project)
                        (map io/file)
                        (b/namespaces-on-classpath :classpath)
                        (sort))]
    (mapcat #(matching-nses % avail-nses) nses)))

(defn compilable-namespaces
  "Returns a seq of the namespaces that are compilable, regardless of whether
  their class files are present and up-to-date."
  [{:keys [aot source-paths] :as project}]
  (if (or (= :all aot) (= [:all] aot))
    (sort (b/namespaces-on-classpath :classpath (map io/file source-paths)))
    (find-namespaces-by-regex project aot)))

(defn stale-namespaces
  "Return a seq of namespaces that are both compilable and that have missing or
  out-of-date class files."
  [project]
  (for [namespace (compilable-namespaces project)
        :let [[rel-source source]
              (or (first (for [source-path (:source-paths project)
                               rel-source (map (partial b/path-for namespace) ["clj" "cljc"])
                               :let [file (io/file source-path rel-source)]
                               :when (.exists ^File file)]
                           [rel-source file]))
                  (let [rel-source (b/path-for namespace)]
                    ;; always return a source file location (#1205)
                    [rel-source (io/file (first (:source-paths project)) rel-source)]))]
        :when source
        :let [rel-compiled (.replaceFirst rel-source "\\.cljc?$" "__init.class")
              compiled (io/file (:compile-path project) rel-compiled)]
        :when (>= (.lastModified source) (.lastModified compiled))]
    namespace))

;; .class file cleanup

(defn- package-in-project?
  "Tests if the package found in the compile path exists as a directory in the
  source path."
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
  (let [path (.replace parent compile-path source-path)]
    (or (.exists (io/file (str path ".clj")))
        (.exists (io/file (str path ".cljc"))))))

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

(defn compilation-specs [cli-args]
  (if (contains? #{[:all] [":all"]} cli-args)
    [:all]
    (->> cli-args
         (map #(if (string? %) (read-string %) %))
         (sort-by (comp not regex?)))))

(def ^:private thread-factory-form
  `(let [counter# (atom 0)]
     (proxy [java.util.concurrent.ThreadFactory] []
      (newThread [r#]
        (let [thread-factory# (java.util.concurrent.Executors/defaultThreadFactory)]
          (doto (.newThread thread-factory# r#)
            (.setName (str "leiningen-send-off-pool-" (swap! counter# inc)))))))))

(def ^:private set-agent-threadpool-form
  ;; set-agent-send-off-executor! was introduced in Clojure 1.5
  `(when-let [set-executor!# (resolve 'clojure.core/set-agent-send-off-executor!)]
     (set-executor!#
      (doto ^java.util.concurrent.ThreadPoolExecutor
          (java.util.concurrent.Executors/newCachedThreadPool ~thread-factory-form)
        (.setKeepAliveTime 100 java.util.concurrent.TimeUnit/MILLISECONDS)))))

(defn compile
  "Compile Clojure source into .class files.

Uses the namespaces specified under :aot in project.clj or those given
as command-line arguments. Use :all argument to compile everything. Pass
#\"regular expressions\" to compile any matching namespaces. You may need
to escape punctuation for your shell.

This should automatically happen when required if it's configured correctly; it
shouldn't need to be manually invoked. See the javac task as well.

Compiling code loads the namespace, so keep side-effects out of the top level.
Code that should run on startup belongs in a -main defn."
  ([project]
   (if-let [namespaces (seq (stale-namespaces project))]
     (let [ns-sym (gensym "namespace")
           form `(do
                   ~set-agent-threadpool-form
                   (doseq [~ns-sym '~namespaces]
                     ~(when main/*info*
                        `(binding [*out* *err*]
                           (println "Compiling" ~ns-sym)))
                     (try
                       (clojure.core/compile ~ns-sym)
                       (catch Throwable t#
                         (binding [*out* *err*]
                           (println (.getMessage t#)))
                         (throw t#)))))
           project (update-in project [:prep-tasks]
                              (partial remove #{"compile"}))]
       (try (binding [eval/*eval-print-dup* true]
              (eval/eval-in-project project form))
            (catch Exception e
              (main/abort "Compilation failed:" (.getMessage e)))
            (finally (clean-non-project-classes project))))
     (main/debug "All namespaces already AOT compiled.")))
  ([project & args]
   (compile (assoc project :aot (compilation-specs args)))))
