(ns org.domain.tricky-name.munch)

(defn -main [& args]
  (spit (format "%s/lein-test" (System/getProperty "java.io.tmpdir"))
        (pr-str :munched args)))
