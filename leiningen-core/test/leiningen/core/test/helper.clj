(ns leiningen.core.test.helper)

(defn abort-msg
  "Catches main/abort thrown by calling f on its args and returns its error
 message."
  [f & args]
  (with-out-str
    (binding [*err* *out*]
      (try
        (apply f args)
        (catch clojure.lang.ExceptionInfo e
          (when-not (:exit-code (ex-data e))
            (throw e)))))))
