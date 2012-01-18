(ns leiningen.test.uberjar
  (:use [leiningen.uberjar] :reload)
  (:use [clojure.test]
        [leiningen.jar :only [jar]]
        [leiningen.core :only [read-project]]
                [leiningen.test.helper :only [sample-no-aot-project]])
  (:import [java.util.zip ZipFile]))

(deftest test-uberjar
  (let [filename "TODO"
        _ (with-out-str (uberjar sample-no-aot-project))
        entries (->> (ZipFile. filename)
                    .entries
                    enumeration-seq
                    (map (memfn getName))
                    set)]
    (is (entries "nom/nom/nom.clj"))
    (is (entries "org/codehaus/janino/Compiler$1.class"))
    (is (not (some #(re-find #"dummy" %) entries)))))

(doseq [[_ var] (ns-publics *ns*)] (alter-meta! var assoc :busted true))