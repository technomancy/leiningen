(ns leiningen.core.utils)

(defn read-file
  "Read the contents of file if it exists."
  [file]
  (when (.exists file)
    (read-string (slurp file))))
