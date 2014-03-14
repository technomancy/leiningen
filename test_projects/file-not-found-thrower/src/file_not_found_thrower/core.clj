(ns file-not-found-thrower.core)

(defn -main
  "I don't do a whole lot."
  []
  (slurp "haha this file does NOT EXIST"))
