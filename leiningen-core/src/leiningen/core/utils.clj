(ns leiningen.core.utils
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str])
  (:import (com.hypirion.io RevivableInputStream)
           (clojure.lang LineNumberingPushbackReader)
           (java.io ByteArrayOutputStream PrintStream File FileDescriptor
                    FileOutputStream FileInputStream InputStreamReader)
           (java.net URL)
           (java.nio.file Files)
           (java.nio.file.attribute PosixFilePermissions)))

(defn create-tmpdir
  "Creates a temporary directory in parent (something clojure.java.io/as-path
  can handle) with the specified permissions string (something
  PosixFilePermissions/asFileAttribute can handle i.e. \"rw-------\") and
  returns its Path."
  [parent prefix permissions]
  (let [nio-path (.toPath (io/as-file parent))
        perms (PosixFilePermissions/fromString permissions)
        attr (PosixFilePermissions/asFileAttribute perms)]
    (Files/createTempDirectory nio-path prefix (into-array [attr]))))

(def rebound-io? (atom false))

(defn rebind-io! []
  (when-not @rebound-io?
    (let [new-in (-> FileDescriptor/in FileInputStream. RevivableInputStream.)]
      (System/setIn new-in)
      (.bindRoot #'*in* (-> new-in InputStreamReader.
                            LineNumberingPushbackReader.)))
    (reset! rebound-io? true)))

(defn build-url
  "Creates java.net.URL from string"
  ^URL [url]
  (try (URL. url)
       (catch java.net.MalformedURLException _
         (URL. (str "http://" url)))))

(defmacro with-write-permissions
  "Runs body only if path is writeable, or - if it does not already exist - can
  be created."
  [path & body]
  `(let [p# ~path
         f# (new File p#)]
     (if (or (and (.exists f#) (.canWrite f#))
             (and (not (.exists f#)) (some-> f# .getParentFile .canWrite)))
       (do ~@body)
       (throw (java.io.IOException.
         (str "Permission denied. Please check your access rights for " p#))))))

(defn read-file
  "Returns the first Clojure form in a file if it exists."
  [file]
  (if (.exists file)
    (try (read-string (slurp file))
        (catch Exception e
         (binding [*out* *err*] ;; TODO: use main/warn for this in 3.0
           (println "Error reading"
                   (.getName file)
                   "from"
                   (.getParent file))
           (if (zero? (.length file))
             (println "File cannot be empty")
             (if (.contains (.getMessage e) "EOF while reading")
               (println "Invalid content was found")
               (println (.getMessage e)))))))))

(defn symlink?
  "Checks if a File is a symbolic link or points to another file."
  [file]
  (let [canon (if-not (.getParent file)
                file
                (-> (.. file getParentFile getCanonicalFile)
                    (File. (.getName file))))]
    (not= (.getCanonicalFile canon)
          (.getAbsoluteFile canon))))

(defn mkdirs
  "Make a given directory and its parents, but throw an Exception on failure."
  [f] ; whyyyyy does .mkdirs fail silently ugh
  (when f ; (io/file nil) returns nil, so we mimic a similar API instead of failing eagerly on nil inputs.
    (when-not (or (.mkdirs (io/file f)) (.exists (io/file f)))
      (throw (Exception. (str "Couldn't create directories: " (io/file f)))))))

(defn relativize
  "Makes the filepath path relative to base. Assumes base is an ancestor to
  path, and that the path contains no '..'."
  [base path]
  ;; TODO: When moving to Java 1.7, use Path's relativize instead
  (let [base-uri (.toURI (io/file base))
        path-uri (.toURI (io/file path))]
    (.. base-uri (relativize path-uri) (getPath))))

(defn ns-exists? [namespace]
  (or (find-ns (symbol namespace))
      (some (fn [suffix]
              (-> (#'clojure.core/root-resource namespace)
                 (subs 1)
                 (str suffix)
                 io/resource))
            [".clj" ".cljc" (str clojure.lang.RT/LOADER_SUFFIX ".class")])))

(defn error [& args]
  (binding [*out* *err*] ;; TODO: use main/warn for this in 3.0
    (apply println "Error:" args)))

(defn require-resolve
  "Resolve a fully qualified symbol by first requiring its namespace."
  ([sym]
     (if-let [ns (namespace sym)]
       (when (ns-exists? ns)
         (let [ns (symbol ns)]
           (when-not (find-ns ns)
             (require ns)))
         (resolve sym))
       (resolve sym)))
  ([ns sym] (require-resolve (symbol ns sym))))

;; # OS detection

(defn- get-by-pattern
  "Gets a value from map m, but uses the keys as regex patterns, trying
  to match against k instead of doing an exact match."
  [m k]
  (m (first (drop-while #(nil? (re-find (re-pattern %) k))
                        (keys m)))))

(defn- get-with-pattern-fallback
  "Gets a value from map m, but if it doesn't exist, fallback
   to use get-by-pattern."
  [m k]
  (let [exact-match (m k)]
    (if (nil? exact-match)
      (get-by-pattern m k)
      exact-match)))

(def ^:private native-names
  {"Mac OS X" :macosx "Windows" :windows "Linux" :linux
   "FreeBSD" :freebsd "OpenBSD" :openbsd
   "amd64" :x86_64 "x86_64" :x86_64 "x86" :x86 "i386" :x86
   "arm" :arm "SunOS" :solaris "sparc" :sparc "Darwin" :macosx "aarch64" :aarch_64})

(defn get-os
  "Returns a keyword naming the host OS."
  []
  (get-with-pattern-fallback native-names (System/getProperty "os.name")))

(defn get-arch
  "Returns a keyword naming the host architecture"
  []
  (get-with-pattern-fallback native-names (System/getProperty "os.arch")))

(defn platform-nullsink
  "Returns a file destination that will discard output."
  []
  (io/file (if (= :windows (get-os))
             "NUL"
             "/dev/null")))


;; The ordering on map-vals and filter-vals may seem strange, but it helps out
;; if you want to do stuff like (update m :foo map-vals f extra-args)

(defn map-vals
  "Like 'update', but for all values in a map."
  [m f & args]
  (zipmap (keys m) (map #(apply f % args) (vals m))))

(defn filter-vals
  "Like filter, but for values over a map: If pred is satisfied on a value in m,
  then its entry is preserved, otherwise it is removed."
  [m pred]
  (->> (filter #(pred (val %)) m)
       (into {})))

;; # Git

;; This is very similar to the read-file function above. The only differences
;; are the error messages and the transformations done on the content.
(defn- git-file-contents
  "Returns the (trimmed) contents by the given git path, or nil if it is
  inacessible or nonexisting. If it exists and is not readable, a warning is
  printed."
  [git-dir ref-path]
  (let [ref (io/file git-dir ref-path)]
    (if (.canRead ref)
      (.trim (slurp ref))
      (do
        (when (.exists ref)
          (binding [*out* *err*] ;; TODO: use main/warn for this in 3.0
            (println "Warning: Contents of git file"
                     (str ".git/" ref-path) "is not readable.")
            (println "(Check that you have the right permissions to read"
                     "the .git repo)")))
        nil))))

(defn ^:internal resolve-git-dir [project]
  (let [alternate-git-root (io/file (get-in project [:scm :dir]))
        git-dir-file (io/file (or alternate-git-root (:root project)) ".git")]
    (if (and (.isFile git-dir-file) (.canRead git-dir-file))
      (io/file (second (re-find #"gitdir: (\S+)" (slurp (str git-dir-file)))))
      git-dir-file)))

(defn- read-git-ref
  "Reads the commit SHA1 for a git ref path, or nil if no commit exist."
  [git-dir ref-path]
  (git-file-contents git-dir ref-path))

(defn- read-git-head-file
  "Reads the current value of HEAD by attempting to read .git/HEAD, returning
  the SHA1 or nil if none exists."
  [git-dir]
  (some->> (git-file-contents git-dir "HEAD")
           (re-find #"ref: (\S+)")
           (second)
           (read-git-ref git-dir)))

;; TODO: de-dupe with pom namespace (3.0?)

(defn ^:internal read-git-head
  "Reads the value of HEAD and returns a commit SHA1, or nil if no commit
  exist."
  [git-dir]
  (try
    (let [git-ref (sh/sh "git" "rev-parse" "HEAD" :dir git-dir)]
      (if (= (:exit git-ref) 0)
        (.trim (:out git-ref))
        (read-git-head-file git-dir)))
    (catch java.io.IOException e (read-git-head-file git-dir))))

(defn last-distinct
  "Like distinct, but retains the last version instead of the first version of a
  duplicate."
  [coll]
  (reverse (distinct (reverse coll))))

;; Inspired by distinct-by from medley (https://github.com/weavejester/medley),
;; also under the EPL 1.0.
(defn last-distinct-by
  "Returns a lazy sequence of the elements of coll, removing any
  elements that return duplicate values when passed to a function f.
  Only the last element that is a duplicate is preserved."
  [f coll]
  (let [step (fn step [xs seen]
               (lazy-seq
                ((fn [[x :as xs] seen]
                   (when-let [s (seq xs)]
                     (let [fx (f x)]
                       (if (contains? seen fx)
                         (recur (rest s) seen)
                         (cons x (step (rest s) (conj seen fx)))))))
                 xs seen)))]
    (reverse (step (reverse coll) #{}))))

(defn ancestor?
  "Is a an ancestor of b?"
  [a b]
  (let [hypothetical-ancestor (.getCanonicalPath (io/file a))
        hypothetical-descendant (.getCanonicalPath (io/file b))]
    (and (.startsWith hypothetical-descendant hypothetical-ancestor)
         (not (= hypothetical-descendant hypothetical-ancestor)))))

(defmacro with-system-out-str
  "Like with-out-str, but for System/out."
  [& body]
  `(try (let [o# (ByteArrayOutputStream.)]
          (System/setOut (PrintStream. o#))
          ~@body
          (.toString o#))
     (finally
       (System/setOut
        (-> FileDescriptor/out FileOutputStream. PrintStream.)))))

(defmacro with-system-err-str
  "Like with-out-str, but for System/err."
  [& body]
  `(try (let [o# (ByteArrayOutputStream.)]
          (System/setErr (PrintStream. o#))
          ~@body
          (.toString o#))
     (finally
       (System/setErr
        (-> FileDescriptor/err FileOutputStream. PrintStream.)))))

(defn strip-properties-comments
  "Takes a string containing serialized java.util.Properties
  and removes all the comment lines (those beginning with #)"
  [s]
  (str/replace s #"(\A|\R)(#.+(\R|\z))+" "$1"))
