(ns leiningen.core.user
  "Functions exposing user-level configuration."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [leiningen.core.utils :as utils])
  (:import (java.util.regex Pattern)))

(defn leiningen-home
  "Return full path to the user's Leiningen home directory."
  []
  (let [lein-home (System/getenv "LEIN_HOME")
        lein-home (or (and lein-home (io/file lein-home))
                      (io/file (System/getProperty "user.home") ".lein"))]
    (.getAbsolutePath (doto lein-home .mkdirs))))

(def init
  "Load the user's ~/.lein/init.clj file, if present."
  (memoize (fn []
             (let [init-file (io/file (leiningen-home) "init.clj")]
               (when (.exists init-file)
                 (try (load-file (.getAbsolutePath init-file))
                      (catch Exception e
                        (.printStackTrace e))))))))

(def profiles-d-profiles
  "Load all Clojure files from the profiles.d folder in your Leiningen home if
  present. Returns a realized seq with the different profiles."
  (memoize
   (fn []
     (let [profile-dir (io/file (leiningen-home) "profiles.d")]
       (when (and (.exists profile-dir) (.isDirectory profile-dir))
         (doall
          (for [file (.listFiles profile-dir)
                :when (.. file getName (endsWith ".clj"))]
            (try (utils/read-file file)
                 (catch Exception e
                   (binding [*out* *err*]
                     (println "Error reading" (.getName file)
                              "from" (str (leiningen-home) "/profiles.d:"))
                     (println (.getMessage e))))))))))))

(def ^:private load-profiles
  "Load profiles.clj from your Leiningen home if present."
  (memoize
   (fn []
     (try (utils/read-file (io/file (leiningen-home) "profiles.clj"))
          (catch Exception e
            (binding [*out* *err*]
              (println "Error reading profiles.clj from" (leiningen-home))
              (println (.getMessage e))))))))

(def profiles
  "Load profiles.clj from your Leiningen home and profiles.d if present."
  (memoize
   (fn []
     (let [error-fn ;; TODO: More descriptive error messages.
           (fn [a b]
             (binding [*out* *err*]
               (println "Error: A profile is defined multiple times!")
               (println "Please check your profiles.clj and your profiles"
                        "in the profiles.d directory."))
             (throw (Exception. "Multiple profiles defined in ~/.lein")))]
       (try (apply merge-with error-fn
                   (load-profiles) (profiles-d-profiles))
            (catch Exception e))))))

(defn gpg-program
  "Lookup the gpg program to use, defaulting to 'gpg'"
  []
  (or (System/getenv "LEIN_GPG") "gpg"))

(defn credentials-fn
  "Decrypt map from credentials.clj.gpg in Leiningen home if present."
  ([] (let [cred-file (io/file (leiningen-home) "credentials.clj.gpg")]
        (if (.exists cred-file)
          (credentials-fn cred-file))))
  ([file]
     (let [{:keys [out err exit]} (try (shell/sh
                                        (gpg-program)
                                        "--quiet" "--batch"
                                        "--decrypt" (str file))
                                       (catch java.io.IOException e
                                         {:exit 1 :err (.getMessage e)}))]
       (if (pos? exit)
         (binding [*out* *err*]
           (println "Could not decrypt credentials from" (str file))
           (println err))
         (read-string out)))))

(def credentials (memoize credentials-fn))

(defn- match-credentials [settings auth-map]
  (get auth-map (:url settings)
       (first (for [[re? cred] auth-map
                    :when (and (instance? Pattern re?)
                               (re-find re? (:url settings)))]
                cred))))

(defn- resolve-credential
  [source-settings result [k v]]
  (letfn [(resolve [v]
            (cond (= :env v)
                  (System/getenv (str "LEIN_" (str/upper-case (name k))))

                  (and (keyword? v) (= "env" (namespace v)))
                  (System/getenv (str/upper-case (name v)))

                  (= :gpg v)
                  (get (match-credentials source-settings (credentials)) k)

                  (coll? v)
                  (->> (map resolve v)
                       (remove nil?)
                       first)
                  :else v))]
    (assoc result k (resolve v))))

(defn resolve-credentials
  "Applies credentials from the environment or ~/.lein/credentials.clj.gpg
   as they are specified and available."
  [settings]
  (let [gpg-creds (when (= :gpg (:creds settings))
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
