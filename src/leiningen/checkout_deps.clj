(ns leiningen.checkout-deps
  (:use [leiningen.core :only [read-project]]
        [clojure.java.io :only [file]]))

(defn checkout-deps-paths [project]
  (apply concat (for [dep (.listFiles (file (:root project) "checkouts"))]
                  ;; Note that this resets the leiningen.core/project var!
                  (let [proj (binding [*ns* (find-ns 'leiningen.core)]
                               (read-project (.getAbsolutePath
                                              (file dep "project.clj"))))]
                      (for [d [:source-path :compile-path :resources-path]]
                        (proj d))))))

(defn checkout-deps [project]
  ;; TODO: look at all deps that have version-control repo information
  ;; in their jar and check them out into the checkouts directory.
  )
