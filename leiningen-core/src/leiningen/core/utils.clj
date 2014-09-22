(ns leiningen.core.utils
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh])
  (:import (com.hypirion.io RevivableInputStream)
           (clojure.lang LineNumberingPushbackReader)
           (java.io File FileDescriptor FileInputStream InputStreamReader)
           (java.net URL)))

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
  [url]
  (try (URL. url)
       (catch java.net.MalformedURLException _
         (URL. (str "http://" url)))))

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
                   (.getParent file)))
         (throw e)))))

(defn symlink?
  "Checks if a File is a symbolic link or points to another file."
  [file]
  (let [canon (if-not (.getParent file)
                file
                (-> (.. file getParentFile getCanonicalFile)
                    (File. (.getName file))))]
    (not= (.getCanonicalFile canon)
          (.getAbsoluteFile canon))))

(defn ns-exists? [namespace]
  (some (fn [suffix]
          (-> (#'clojure.core/root-resource namespace)
              (subs 1)
              (str suffix)
              io/resource))
        [".clj" (str clojure.lang.RT/LOADER_SUFFIX ".class")]))

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
   "arm" :arm "SunOS" :solaris "sparc" :sparc "Darwin" :macosx})

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

(defn map-vals [m f & args]
  (zipmap (keys m) (map #(apply f % args) (vals m))))

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
        (:out git-ref)
        (read-git-head-file git-dir)))
    (catch java.io.IOException e (read-git-head-file git-dir))))
