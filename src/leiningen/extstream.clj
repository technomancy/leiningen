(ns leiningen.extstream
  "Extends java.io.InputStream by reporting to console output
the percentage of the stream that has been consumed after each read."
  (:gen-class
   :extends java.io.InputStream
   :state state
   :init init
   :constructors {[java.io.InputStream Long] []}
   :methods [[saveTotalBytes [Long] void]
             [printProgress [] void]]
   :main false))

(defn -init [stream content-length]
  [ [] (atom (into {} {:stream stream
                      :content-length content-length
                      :total-bytes 0})) ])


(defn- -saveTotalBytes [this total-bytes ]
  (let [state-map (.state this)]
    (reset! state-map (assoc @state-map
                        :total-bytes total-bytes))))


(defn- -printProgress [this]
  (let [state @(.state this)
        total-bytes (:total-bytes state)
        content-length (:content-length state)
        progress (/ (* total-bytes 100.0) content-length)
        ending (if-not (== progress 100) "\r" "\n")]
    (printf "%.1f%% complete%s" progress ending)
    (flush)))


(defn -read-byte<> [this bytebuf]
  (let [state @(.state this)
        stream (:stream state)
        count (.read stream bytebuf)]
    (-saveTotalBytes this (+ (:total-bytes state) count))
    (-printProgress this)
    count))


(defn -read-byte<>-int-int [this bytebuf off len]
  (let [state @(.state this)
        stream (:stream state)
        count (.read stream bytebuf off len)]
    (-saveTotalBytes this (+ (:total-bytes state) count))
    (-printProgress this)
    count))
