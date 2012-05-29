(ns leiningen.core.user
  "Functions exposing user-level configuration."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn leiningen-home
  "Return full path to the user's Leiningen home directory."
  []
  (let [lein-home (System/getenv "LEIN_HOME")
        lein-home (or (and lein-home (io/file lein-home))
                      (io/file (System/getProperty "user.home") ".lein"))]
    (.getAbsolutePath (doto lein-home .mkdirs))))

;; TODO: is this still needed now that we have the user profile?
(def init
  "Load the user's ~/.lein/init.clj file, if present."
  (memoize (fn []
             (let [init-file (io/file (leiningen-home) "init.clj")]
               (when (.exists init-file)
                 (try (load-file (.getAbsolutePath init-file))
                      (catch Exception e
                        (.printStackTrace e))))))))

(defn profiles []
  (let [profiles-file (io/file (leiningen-home) "profiles.clj")]
    (if (.exists profiles-file)
      (read-string (slurp profiles-file)))))

(defn credentials
  ([] (let [cred-file (io/file (leiningen-home) "credentials.clj.gpg")]
        (when (.exists cred-file)
         (credentials cred-file))))
  ([file]
     (let [{:keys [out err exit]} (try (shell/sh "gpg" "--batch" "--quiet"
                                                 "--decrypt" (str file))
                                       (catch java.io.IOException e
                                         {:exit 1 :err (.getMessage e)}))]
       (if (pos? exit)
         (binding [*out* *err*]
           (println "Could not decrypt credentials from" (str file))
           (println err))
         (read-string out)))))
