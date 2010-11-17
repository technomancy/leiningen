(ns org.domain.tricky-name.brunch)

(defn -main [& args]
  (spit (format "%s/lein-test" (System/getProperty "java.io.tmpdir")) "BRUNCH"))
