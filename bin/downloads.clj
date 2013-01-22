(ns leiningen.downloads
  "Calculate download statistics from logs."
  (:require [clojure.java.io]
            [clojure.pprint :refer [pprint]]
            [clojure.java.shell :refer [sh]]))

;; Before GitHub shut down its download service all uberjars were
;; hosted there. Here's the latest data we have on it.
(def github {"leiningen-1.6.1.1-standalone.jar" 15143,
             "leiningen-1.6.2-standalone.jar" 16640,
             "leiningen-1.7.1-standalone.jar" 64026,
             "leiningen-full.jpg" 519,
             "leiningen-1.5.2-standalone.jar" 24865,
             "leiningen-1.6.1-standalone.jar" 9405,
             "leiningen-1.7.0-standalone.jar" 10969,
             "leiningen-1.4.2-standalone.jar" 31651,
             "leiningen-1.5.1-standalone.jar" 290,
             "leiningen-1.6.0-standalone.jar" 1065,
             "leiningen-1.4.1-standalone.jar" 1606,
             "leiningen-1.5.0-standalone.jar" 9575,
             "leiningen-1.3.1-standalone.jar" 7905,
             "leiningen-1.4.0-standalone.jar" 1589,
             "leiningen-1.3.0-SNAPSHOT-standalone.jar" 280,
             "leiningen-1.4.0-SNAPSHOT-standalone.jar" 423,
             "leiningen-1.3.0-standalone.jar" 2442,
             "leiningen-banner.png" 399328,
             "leiningen-1.2.0-standalone.jar" 3617,
             "leiningen-1.1.0-standalone.jar" 12858,
             "leiningen-1.7.0-SNAPSHOT-standalone.jar" 434,
             "leiningen-1.6.2-SNAPSHOT-standalone.jar" 637,
             "leiningen-1.7.1-SNAPSHOT-standalone.jar" 971,
             "leiningen-2.0.0-preview10-standalone.jar" 555530, ; huh?
             "leiningen-1.4.0-RC2-standalone.jar" 188,
             "leiningen-1.5.0-RC1-standalone.jar" 177,
             "leiningen-2.0.0-preview10-standalone.jar.asc" 272,
             "leiningen-1.4.0-RC1-standalone.jar" 200,
             "leiningen-1.3.0-RC1-standalone.jar" 103,
             "leiningen-2.0.0-preview9-standalone.jar" 442,
             "leiningen-2.0.0-preview8-standalone.jar" 2050,
             "leiningen-2.0.0-preview7-standalone.jar" 8022,
             "leiningen-2.0.0-preview6-standalone.jar" 2839,
             "leiningen-2.0.0-preview9-standalone.jar.asc" 41,
             "leiningen-1.4.0-win32.zip" 70,
             "leiningen-2.0.0-preview8-standalone.jar.asc" 37,
             "leiningen-1.5.0-win32.zip" 464,
             "leiningen-1.4.1-win32.zip" 260,
             "leiningen-2.0.0-preview5-standalone.jar" 200,
             "leiningen-1.4.2-win32.zip" 1108,
             "leiningen-2.0.0-preview4-standalone.jar" 1701,
             "leiningen-2.0.0-preview3-standalone.jar" 2029,
             "leiningen-2.0.0-preview2-standalone.jar" 1437,
             "leiningen-1.5.2-win.zip" 4346,
             "lein-win32.zip" 1502,
             "leiningen-2.0.0-preview1-standalone.jar" 282})

;; filter out non-release-jars
(def github-releases
  (into {} (remove (comp (partial re-find #"SNAPSHOT|RC|zip|jpg|png|asc") key)
                   github)))

(def total (apply + (vals github-releases))) ; 788178

(defn file-for-line [line]
  (let [[_ file] (re-find #"\"GET ([^ ]+) " line)]
    (if file
      (last (.split file "/")))))

(defn parse-line [sums line]
  (if-let [file (file-for-line line)]
    (update-in sums [file] (fnil inc 0))
    sums))

(defn parse-file [f]
  (with-open [rdr (clojure.java.io/reader f)]
    (reduce parse-line {} (line-seq rdr))))

(defn parse-dir [d]
  (apply merge-with + (->> (.listFiles (java.io.File. d))
                           (filter (memfn isFile))
                           (map parse-file))))

;; TODO: fetch S3 logs?
(def -main parse-dir)
