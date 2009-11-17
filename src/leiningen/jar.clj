(ns leiningen.jar
  (:require [leiningen.compile :as compile]
            [lancet]))

(defn jar [project & args]
  (compile/compile project)
  (let [jarfile (str (:root project) "/" (:name project) ".jar")]
    ;; TODO: add manifest
    (lancet/jar {:jarfile jarfile}
                ;; TODO: support slim, etc
                (lancet/fileset {:dir *compile-path*})
                (lancet/fileset {:dir (str (:root project) "/src")})
                (lancet/fileset {:file (str (:root project) "/project.clj")}))
    jarfile))
