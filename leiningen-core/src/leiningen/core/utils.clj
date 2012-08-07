(ns leiningen.core.utils)

(defn read-file
  "Read the contents of file if it exists."
  [file]
  (if (.exists file)
    (read-string (slurp file))))
