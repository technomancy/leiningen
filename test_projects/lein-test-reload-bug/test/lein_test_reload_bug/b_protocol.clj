(prn "loading" 'lein-test-reload-bug.b-protocol)

(ns lein-test-reload-bug.b-protocol)

(defprotocol B
  (b [this]))
