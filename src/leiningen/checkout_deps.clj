(ns leiningen.checkout-deps
  (:use [leiningen.core :only [read-project]]
        [clojure.contrib.java-utils :only [file]]))

(defn checkout-deps-paths [project]
  (apply concat (for [dep (.listFiles (file (:root project) "checkouts"))]
                  ;; Note that this resets the leiningen.core/project var!
                  (let [proj (read-project (.getAbsolutePath
                                            (file dep "project.clj")))]
                      (for [d [:source-path :compile-path :resources-path]]
                        (proj d))))))

(defn checkout-deps [project]
  ;; TODO: look at all deps that have version-control repo information
  ;; in their jar and check them out into the checkouts directory.
  )
