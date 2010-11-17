(ns org.domain.tricky-name.core)

(defn -main [& args]
  (when-not (empty? args)
    (spit (format "%s/lein-test" (System/getProperty "java.io.tmpdir"))
          (str "nom:" (first args)))
    (recur (rest args))))
