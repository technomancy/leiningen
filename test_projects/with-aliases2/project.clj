;; This project is used for leiningen's test suite, so don't change
;; any of these values without updating the relevant tests. If you
;; just want a basic project to work from, generate a new one with
;; "lein new".

(defproject project-with-aliases "0.1.0-SNAPSHOT"
  :a 1
  :profiles {:a2 {:a 2}})
