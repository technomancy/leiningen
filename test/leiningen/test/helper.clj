(ns leiningen.test.helper
  (:require [leiningen.core.project :as project]
            [leiningen.core.user :as user]
            [leiningen.core.utils :as utils]
            [leiningen.core.main :as main]
            [leiningen.core.test.helper :as helper]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; TODO: fix
(def local-repo (io/file (System/getProperty "user.home") ".m2" "repository"))

(def tmp-dir (System/getProperty "java.io.tmpdir"))

(defn m2-dir
  ([n]
   (let [group (-> (if (string? n) n (or (namespace n) (name n)))
                   (str/replace "." "/"))]
     (io/file local-repo group (name n))))
  ([n v]
   (io/file (m2-dir n) v)))

(defn m2-file [n v classifier]
  (io/file (m2-dir n v) (str (name n) "-" v "-" classifier ".jar")))

(defn read-test-project-with-user-profiles [name user-profiles]
  (with-redefs [user/profiles (constantly user-profiles)]
    (let [project (project/read (format "test_projects/%s/project.clj" name))]
      (project/init-project
       (project/project-with-profiles-meta
        project (merge @project/default-profiles (:profiles project)))))))

(defn read-test-project [name]
  (read-test-project-with-user-profiles name {}))

(def with-resources-project (read-test-project "with-resources"))

(def with-aliases-project (read-test-project "with-aliases"))

(def with-aliases2-project (binding [main/*info* false]
                             (read-test-project "with-aliases2")))

(def sample-project (read-test-project "sample"))

(def sample-failing-project (read-test-project "sample-failing"))

(def sample-no-aot-project (read-test-project "sample-no-aot"))

(def sample-ordered-aot-project (read-test-project "sample-ordered-aot"))

(def sample-profile-meta-project (read-test-project "sample-profile-meta"))

(def sample-reader-cond-project (read-test-project "sample-reader-cond"))

(def sample-fixture-error-project (read-test-project "sample-fixture-error"))

(def sample-deploy-project (read-test-project "sample-deploy"))

(def tricky-name-project (read-test-project "tricky-name"))

(def native-project (read-test-project "native"))

(def provided-project (read-test-project "provided"))

(def uberjar-merging-project (read-test-project "uberjar-merging"))

(def data-readers-backwards-compatibility-project (read-test-project "data-readers-backwards-compatibility"))

(def overlapped-sourcepaths-project (read-test-project "overlapped-sourcepaths"))

(def more-gen-classes-project (read-test-project "more-gen-classes"))

(def bad-require-project (read-test-project "bad-require"))

(def java-main-project (read-test-project "java-main"))

(def file-not-found-thrower-project (read-test-project "file-not-found-thrower"))

(def jvm-opts-project (read-test-project "jvm-opts"))

(def with-classifiers-project (read-test-project "with-classifiers"))

(def managed-deps-project (read-test-project "managed-deps"))

(def managed-deps-snapshot-project (read-test-project "managed-deps-snapshot"))

(def with-pom-plugins-project (read-test-project "with-pom-plugins"))

(def lein-test-reload-bug-project (read-test-project "lein-test-reload-bug"))

(def lein-test-exit-code-project (read-test-project "lein-test-exit-code"))

(def leaky-composite-project (read-test-project "leaky-composite"))

(defn abort-msg
  "Catches main/abort thrown by calling f on its args and returns its error
  message."
  [f & args]
  (apply helper/abort-msg f args))

;; grumble, grumble; why didn't this make it into clojure.java.io?
(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents.
  Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (System/gc) ; This sometimes helps release files for deletion on windows.
  (let [f (io/file f)]
    (if (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (io/delete-file f silently)))


(defn fix-path-delimiters [input-str]
  (clojure.string/replace input-str "/" java.io.File/separator))

;; So paths would work under Windows too, which adds a drive letter and changes
;; the path separator.
(defn pathify
  "Converts paths to absolute paths. Will throw if not, because if the path is
  not absolute, then .getAbsolutePath will resolve them relative to current
  directory." [in-str-or-file]
  (cond (or
          (nil? in-str-or-file)
          (not (or
                 (.startsWith in-str-or-file "/")
                 (and
                   (>= (.length in-str-or-file) 3)
                   (= ":\\" (.substring in-str-or-file 1 3))))))
    (throw (RuntimeException. (str "Bad usage, passed: `" in-str-or-file "`.")))
    :else
    (.getAbsolutePath (io/as-file in-str-or-file))))

(defn entries
  "Returns a lazy seq of all the entries in a zipfile."
  [zipfile]
  (enumeration-seq (.entries zipfile)))

(defn walkzip
  "Applies f to all ZipEntries in the ZipFile filename and returns the result as
  a vector."
  [filename f]
  (with-open [z (java.util.zip.ZipFile. filename)]
    (reduce #(conj %1 (f %2)) [] (entries z))))

(defn noerr-fixture [f]
  (binding [*err* (java.io.StringWriter.)]
    (f)))

(def #^{:macro true} with-system-out-str #'utils/with-system-out-str)

(def #^{:macro true} with-system-err-str #'utils/with-system-err-str)

(defn unmemoize [v underlying]
  (alter-var-root v (constantly underlying)))
