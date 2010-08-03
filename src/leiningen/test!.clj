(ns leiningen.test!
  (:refer-clojure :exclude [test])
  (:use [leiningen.clean :only [clean]]
        [leiningen.deps :only [deps]]
        [leiningen.test :only [test]]))

(defn test!
  "Run a project's tests after cleaning and pulling in fresh dependencies."
  [project]
  (doto project clean deps test))
