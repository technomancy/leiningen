(ns leiningen.core.user
  "Functions exposing user-level configuration."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [leiningen.core.utils :as utils])
  (:import (com.hypirion.io Pipe)
           (org.apache.commons.io.output TeeOutputStream)
           (java.util.regex Pattern)
           (java.io ByteArrayOutputStream)))

(defn getprop
  "Wrap System/getProperty for testing purposes."
  [prop-name]
  (System/getProperty prop-name))

(defn getenv
  "Wrap System/getenv for testing purposes."
  [name]
  (System/getenv name))

(defn leiningen-home
  "Return full path to the user's Leiningen home directory."
  []
  (let [lein-home (getenv "LEIN_HOME")
        lein-home (or (and lein-home (io/file lein-home))
                      (io/file (System/getProperty "user.home") ".lein"))]
    (.getAbsolutePath (doto lein-home utils/mkdirs))))

;; TODO: move all these memoized fns into delays
(def init
  "Load the user's ~/.lein/init.clj file, if present."
  (memoize (fn []
             (let [init-file (io/file (leiningen-home) "init.clj")]
               (when (.exists init-file)
                 (try (load-file (.getAbsolutePath init-file))
                      (catch Exception e
                        (.printStackTrace e))))))))

(defn- load-profiles-d-file
  "Returns a map entry containing the filename (without `.clj`) associated
  with its contents. The content will be tagged with its origin."
  [file]
  (try
    (let [kw (->> file .getName (re-find #".+(?=\.clj)") keyword)
          contents (vary-meta (utils/read-file file) ;; assumes the file exist
                              merge {:origin (.getAbsolutePath file)})]
      [kw contents])
    (catch Exception e
      (binding [*out* *err*]
        (println "Error reading" (.getName file)
                 "from" (-> file .getParentFile .getAbsolutePath (str ":")))
        (println (.getMessage e))))))

(def profiles-d-profiles
  "Load all Clojure files from the profiles.d folder in your Leiningen home if
  present. Returns a seq with map entries of the different profiles."
  (memoize
   (fn []
     (let [profile-dir (io/file (leiningen-home) "profiles.d")]
       (if (.isDirectory profile-dir)
         (for [file (.listFiles profile-dir)
               :when (-> file .getName (.endsWith ".clj"))]
           (load-profiles-d-file file)))))))

(def ^:internal load-profiles
  "Load profiles.clj from dir if present. Tags all profiles with its origin."
  (memoize
   (fn [dir]
       (if-let [contents (utils/read-file (io/file dir "profiles.clj"))]
         (utils/map-vals contents vary-meta merge
                         {:origin (str (io/file dir "profiles.clj"))})))))


(def profiles
  "Load profiles.clj from your Leiningen home and profiles.d if present."
  (memoize
   (fn []
     (let [error-fn
           (fn [a b]
             (binding [*out* *err*]
               (println "Error: A profile is defined in both"
                        (-> a meta :origin) "and in" (-> b meta :origin)))
             (throw (ex-info "Multiple profiles defined in ~/.lein"
                             {:exit-code 1})))]
       (if (not (System/getenv "LEIN_NO_USER_PROFILES"))
         (merge-with error-fn
                     (load-profiles (leiningen-home))
                     (into {} (profiles-d-profiles))))))))

(defn gpg-program
  "Lookup the gpg program to use, defaulting to 'gpg'"
  []
  (or (getenv "LEIN_GPG") "gpg"))

(defn- get-english-env
  "Returns env vars as a map with clojure keywords and LANGUAGE set to 'en'"
  []
  (let [env (System/getenv)]
    (assoc (zipmap (map keyword (keys env)) (vals env))
           :LANGUAGE "en")))

(defn- as-env-strings
  [env]
  (into-array String (map (fn [[k v]] (str (name k) "=" v)) env)))

(defn gpg
  "Shells out to (gpg-program) with the given arguments"
  [& args]
  (try
    (let [proc-env (as-env-strings (get-english-env))
          proc-args (into-array String (concat [(gpg-program)] args))
          proc (.exec (Runtime/getRuntime) proc-args proc-env)]
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn [] (.destroy proc))))
      (with-open [out (.getInputStream proc)
                  err-output (ByteArrayOutputStream.)]
        (let [pump-err (doto (Pipe. (.getErrorStream proc)
                                    (TeeOutputStream. System/err err-output))
                         .start)]
          (.join pump-err)
          (let [exit-code (.waitFor proc)]
            {:exit exit-code
             :out (slurp (io/reader out))
             :err (slurp (io/reader (.toByteArray err-output)))}))))
    (catch java.io.IOException e
      {:exit 1 :out "" :err (.getMessage e)})))

(defn gpg-available?
  "Verifies (gpg-program) exists"
  []
  (zero? (:exit (gpg "--version"))))

(defn credentials-fn
  "Decrypt map from credentials.clj.gpg in Leiningen home if present."
  ([] (let [cred-file (io/file (leiningen-home) "credentials.clj.gpg")]
        (if (.exists cred-file)
          (credentials-fn cred-file))))
  ([file]
     (let [{:keys [out err exit]} (gpg "--quiet" "--batch"
                                       "--decrypt" "--" (str file))]
       (if (pos? exit)
         (binding [*out* *err*]
           (println "Could not decrypt credentials from" (str file))
           (println err)
           (println "See `lein help gpg` for how to install gpg."))
         (binding [*read-eval* false]
           (read-string out))))))

(def credentials (memoize credentials-fn))

(defn- match-credentials [settings auth-map]
  (get auth-map (:url settings)
       (first (for [[re? cred] auth-map
                    :when (and (instance? Pattern re?)
                               (re-find re? (:url settings)))]
                cred))))

(defn- resolve-credential
  "Resolve key-value pair from result into a credential, updating result."
  [source-settings result [k v]]
  (letfn [(resolve [v]
            (cond (= :env v)
                  (getenv (str "LEIN_" (str/upper-case (name k))))

                  (and (keyword? v) (= "env" (namespace v)))
                  (getenv (str/upper-case (name v)))

                  (= :gpg v)
                  (get (match-credentials source-settings (credentials)) k)

                  (coll? v) ;; collection of places to look
                  (->> (map resolve v)
                       (remove nil?)
                       first)

                  :else v))]
    (if (#{:username :password :passphrase :private-key-file} k)
      (assoc result k (resolve v))
      (assoc result k v))))

(defn resolve-credentials
  "Applies credentials from the environment or ~/.lein/credentials.clj.gpg
  as they are specified and available."
  [settings]
  (let [gpg-creds (if (= :gpg (:creds settings))
                    (match-credentials settings (credentials)))
        resolved (reduce (partial resolve-credential settings)
                         (empty settings)
                         settings)]
    (if gpg-creds
      (dissoc (merge gpg-creds resolved) :creds)
      resolved)))

(defn profile-auth
  "Look up credentials for a given repository in :auth profile."
  [settings]
  (if-let [repo-auth (-> (profiles) :auth :repository-auth)]
    (merge settings (match-credentials settings repo-auth))
    settings))
