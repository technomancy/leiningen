(ns org.domain.tricky-name.core)

(defn -main [& args]
  (when-not (empty? args)
    (spit "/tmp/lein-test" (str "nom:" (first args)))
    (recur (rest args))))
