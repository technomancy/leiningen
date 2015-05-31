(ns leiningen.fixed-and-var-args "Dummy task for tests.")

(defn fixed-and-var-args [project one two & rest]
  (println "a dummy task for tests"))
