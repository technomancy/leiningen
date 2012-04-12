(ns leiningen.core.user
  "Functions exposing user-level configuration."
  (:require [clojure.java.io :as io]))

(defn leiningen-home
  "Return full path to the user's Leiningen home directory."
  []
  (.getAbsolutePath (doto (io/file (System/getenv "LEIN_HOME")) .mkdirs)))

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