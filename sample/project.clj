;; This project is used for leiningen's test suite, so don't change
;; any of these values without updating the relevant tests. If you
;; just want a basic project to work from, generate a new one with
;; "lein new".
(defproject nomnomnom "0.5.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
                 [rome/rome "0.9"]
                 [org.ccil.cowan.tagsoup/tagsoup "1.2"]]
  :main nom.nom.nom
  :warn-on-reflection true)
