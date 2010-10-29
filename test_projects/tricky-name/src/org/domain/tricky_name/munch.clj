(ns org.domain.tricky-name.munch)

(defn -main [& args]
  (spit "/tmp/lein-test" (pr-str :munched args)))
