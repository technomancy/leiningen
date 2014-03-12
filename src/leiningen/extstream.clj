(ns leiningen.extstream
  (:gen-class
   :extends java.io.InputStream
   :state state
   :init init
   :constructors {[java.io.InputStream Long] []}
   :main false))

(defn -init [stream content-length]
  [ [] (ref (into {} {:stream stream
                      :content-length content-length
                      :total-bytes 0})) ])

(defn print-progress [count length]
  (let [progress (/ (* count 100.0) length)]
    (printf "%.1f%%\r" progress)))

(defn -read-void [this]
  (let [b []
        count (.read (:stream @(.state this)) b)]
    (if (= -1 count) count (first b))))

(defn -read-byte<> [this bytebuf]
  (let [state-map (.state this)
        stream (:stream @state-map)
        content-length (:content-length @state-map)
        count (.read stream bytebuf)]
    count))

(defn -read-byte<>-int-int [this bytebuf off len]
  (let [state-map (.state this)
        stream (:stream @state-map)
        content-length (:content-length @state-map)
        count (.read stream bytebuf off len)]
    count))
