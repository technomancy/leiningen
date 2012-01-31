(ns leiningen.util.plugins
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [leiningen.util.file :as file])
  (:import (java.lang.management ManagementFactory)
           (java.security MessageDigest)
           (java.io File)))

;; This whole namespace is a bit messy, but it's going away in
;; Leiningen 2.0 (with pomegranate), so that's totally A-OK!.

(defn- sha1-digest [content]
  (.toString (BigInteger. 1 (-> (MessageDigest/getInstance "SHA1")
                                (.digest (.getBytes content)))) 16))

(defn ^{:internal true} deps-checksum
  ([project keys] (sha1-digest (pr-str (map project keys))))
  ([project] (deps-checksum project [:dependencies :dev-dependencies])))

;; Split this function out for better testability.
(defn- get-raw-input-args []
  (.getInputArguments (ManagementFactory/getRuntimeMXBean)))

(defn- get-input-args
  "Returns a vector of input arguments, accounting for a bug in RuntimeMXBean
  that splits arguments which contain spaces."
  []
  ;; RuntimeMXBean.getInputArguments() is buggy when an input argument
  ;; contains spaces. For an input argument of -Dprop="hello world" it
  ;; returns ["-Dprop=hello", "world"]. Try to work around this bug.
  (letfn [(join-broken-args [v arg] (if (= \- (first arg))
                                      (conj v arg)
                                      (conj (vec (butlast v))
                                            (format "%s %s" (last v) arg))))]
         (reduce join-broken-args [] (get-raw-input-args))))

(defn- classpath-with-plugins [project plugins]
  (string/join File/pathSeparator
               (concat (for [plugin plugins]
                         (.getAbsolutePath (io/file
                                            (:root plugin) ".lein-plugins"
                                            (.getName (io/file plugin)))))
                       [(System/getProperty "java.class.path")])))

(defn- write-self-trampoline [project plugins]
  ;; Attempt to reconstruct the command by which the current JVM was
  ;; launched. Basically work around the fact that the JVM doesn't
  ;; have $0 and $@.
  (spit (System/getProperty "leiningen.trampoline-file")
        (string/join " " `(~(System/getenv "JAVA_CMD") "-client"
                           ~@(get-input-args)
                           "-cp" ~(classpath-with-plugins project plugins)
                           "clojure.main" "-e"
                           "\"(use 'leiningen.core)(-main)\"" "/dev/null"
                           ~@*command-line-args*))))

(defn plugin-files [project dir]
  ;; Trust me, the alternative is too terrible to comprehend.
  ;; Basically we'd have to restructure half of leiningen.deps and
  ;; leiningen.util.maven to factor out the parts that don't depend on
  ;; the leiningen.core namespace.
  (require 'leiningen.deps)
  (-> ((resolve 'leiningen.deps/do-deps)
       (assoc project :library-path (.getAbsolutePath dir)) :plugins)
      .getDirectoryScanner .getIncludedFiles))

(defn stale? [project dir]
  (and (seq (:plugins project))
       (or (not (.exists (io/file dir "checksum")))
           (not= (deps-checksum project [:plugins])
                 (slurp (io/file dir "checksum"))))))

(defn delete-stale [dir old-plugins new-plugins]
  ;; We can't delete theese earlier because it could interfere
  ;; with dependency fetching in the case of wagons, etc.
  (let [new-set (set (for [p new-plugins] (.getName (io/file p))))]
    (doseq [old old-plugins
            :when (not (new-set old))]
      (.delete (io/file dir old)))))

(defn download-plugins [project]
  (let [dir (io/file (:root project) ".lein-plugins")
        old-plugins (.list dir)]
    (when (stale? project dir)
      (let [plugins (plugin-files project dir)]
        (delete-stale dir old-plugins plugins)
        (spit (io/file dir "checksum")
              (deps-checksum project [:plugins]))
        ;; We can't access the plugins we just downloaded, so we need
        ;; to exit the JVM and relaunch using trampolining.
        (write-self-trampoline project plugins)
        (System/exit 0)))))
