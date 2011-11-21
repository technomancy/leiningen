(ns leiningen.core.user
  "Functions exposing user-level configuration."
  (:require [clojure.java.io :as io]))

(defn leiningen-home
  "Return full path to the user's Leiningen home directory."
  []
  (.getAbsolutePath (doto (if-let [lein-home (System/getenv "LEIN_HOME")]
                            (io/file lein-home)
                            (io/file (or (System/getenv "HOME")
                                         (System/getenv "USERPROFILE")) ".lein"))
                      .mkdirs)))

(def init
  "Load the user's ~/.lein/init.clj file, if present."
  (memoize (fn []
             (let [init-file (io/file (leiningen-home) "init.clj")]
               (when (.exists init-file)
                 (try (load-file (.getAbsolutePath init-file))
                      (catch Exception e
                        (.printStackTrace e))))))))

(defn settings
  "Look up the settings map from init.clj or an empty map if it doesn't exist."
  []
  (if-let [settings-var (resolve 'user/settings)]
    @settings-var
    {}))

(defn plugins
  "Return a list of paths to all plugins the user has installed."
  []
  (for [plugin (.listFiles (io/file (leiningen-home) "plugins"))]
    (.getAbsolutePath plugin)))
