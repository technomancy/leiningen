(ns nom.nom.nom
  (:gen-class))

(when-not (= "1.0.0-" (clojure-version))
  (throw (Exception. (str "Not running Clojure 1.0.0: " (clojure-version)))))

(defn -main [& args]
  (when-not (empty? args)
    (println "NOM! Munched" (first args))
    (recur (rest args))))
