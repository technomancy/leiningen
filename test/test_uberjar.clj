(ns test-uberjar
  (:use [leiningen.uberjar] :reload)
  (:use [clojure.test]
        [leiningen.jar :only [get-jar-filename get-default-uberjar-name jar]]
        [leiningen.core :only [read-project]])
  (:import [java.util.zip ZipFile]))

(def project (binding [*ns* (the-ns 'leiningen.core)]
               (read-project "test_projects/sample_no_aot/project.clj")))

(deftest test-uberjar
  (let [filename (get-jar-filename project (get-default-uberjar-name project))
        _ (with-out-str (uberjar project))
        entries (->> (ZipFile. filename)
                    .entries
                    enumeration-seq
                    (map (memfn getName))
                    set)]
    (is (entries "nom/nom/nom.clj"))
    (is (entries "robert/hooke.clj"))
    (is (not (some #(re-find #"dummy" %) entries)))))
