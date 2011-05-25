(ns leiningen.test.uberjar
  (:use [leiningen.uberjar] :reload)
  (:use [clojure.test]
        [leiningen.jar :only [get-jar-filename get-default-uberjar-name jar]]
        [leiningen.core :only [read-project]]
                [leiningen.test.helper :only [sample-no-aot-project]])
  (:import [java.util.zip ZipFile]))

(deftest test-uberjar
  (let [filename (get-jar-filename sample-no-aot-project
                                   (get-default-uberjar-name
                                    sample-no-aot-project))
        _ (with-out-str (uberjar sample-no-aot-project))
        entries (->> (ZipFile. filename)
                    .entries
                    enumeration-seq
                    (map (memfn getName))
                    set)]
    (is (entries "nom/nom/nom.clj"))
    (is (entries "org/codehaus/janino/Compiler$1.class"))
    (is (not (some #(re-find #"dummy" %) entries)))))
