(ns leiningen.deps
  "Download all dependencies."
  (:require [clojure.java.io :as io]
            [leiningen.clean :as clean]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.user :as user])
  (:import (java.security MessageDigest)
           (java.util.jar JarFile)))

(defn- sha1-digest [content]
  (.toString (BigInteger. 1 (-> (MessageDigest/getInstance "SHA1")
                                (.digest (.getBytes content)))) 16))

(defn- deps-checksum [project]
  (sha1-digest (pr-str (:dependencies project))))

(defn- new-deps-checksum-file [project]
  (io/file (:target-path project) ".lein-deps-sum"))

(defn- has-dependencies? [project]
  (some (comp seq project) [:dependencies :dev-dependencies]))

;; TODO: is this necessary with keeping everything in ~/.m2?
(defn fetch-deps?
  "Should we even bother fetching dependencies?"
  [project]
  (let [deps-checksum-file (new-deps-checksum-file project)]
    (and (has-dependencies? project)
         ;; There's got to be a better way to detect direct invocation?
         (or ;; (= "deps" *current-task*)
          (not (:checksum-deps project (:checksum-deps (user/settings) true)))
          (not (.exists deps-checksum-file))
          (not= (slurp deps-checksum-file) (deps-checksum project))))))

(defn- jar-files [files]
  (for [file files
        :when (.endsWith (.getName file) ".jar")]
    (JarFile. file)))

(defn extract-native-deps [project files]
  (doseq [jar (jar-files files)
          entry (enumeration-seq (.entries jar))
          :when (.startsWith (.getName entry) "native/")]
    (let [f (io/file (:root project) (:native-path project)
                     (subs (.getName entry) (count "native/")))]
      (if (.isDirectory entry)
        (.mkdirs f)
        (io/copy (.getInputStream jar entry) f)))))

(defn deps
  "Download :dependencies and put them in :library-path."
  [project]
  (when (fetch-deps? project)
    (when-not (or (:disable-deps-clean project)
                  (:disable-implicit-clean project))
      (clean/clean project))
    (let [files (classpath/resolve-dependencies project)]
      (extract-native-deps project files)
      (let [checksum-file (new-deps-checksum-file project)]
        (.mkdirs (.getParentFile checksum-file))
        (spit checksum-file (deps-checksum project)))
      files)))
