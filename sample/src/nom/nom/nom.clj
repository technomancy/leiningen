(ns nom.nom.nom
  (:gen-class))

(defn -main [& args]
  (when-not (empty? args)
    (println "NOM! Munched" (first args))
    (recur (rest args))))
