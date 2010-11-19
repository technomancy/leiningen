(ns leiningen.util.os
  (:use [clojure.java.io :only [file]]))

(defn- get-by-pattern
  "Gets a value from map m, but uses the keys as regex patterns, trying
   to match against k instead of doing an exact match."
  [m k]
  (m (first (drop-while #(nil? (re-find (re-pattern %) k))
                        (keys m)))))

(def native-names
     {"Mac OS X" :macosx
      "Windows" :windows
      "Linux" :linux
      "FreeBSD" :freebsd
      "SunOS" :solaris
      "OpenBSD" :openbsd
      "amd64" :x86_64
      "x86_64" :x86_64
      "x86" :x86
      "i386" :x86
      "arm" :arm
      "sparc" :sparc})

(defn get-os
  "Returns a keyword naming the host OS."
  []
  (get-by-pattern native-names (System/getProperty "os.name")))

(defn get-arch
  "Returns a keyword naming the host architecture"
  []
  (get-by-pattern native-names (System/getProperty "os.arch")))

(defn platform-nullsink []
  (file (if (= :windows (get-os))
          "NUL"
          "/dev/null")))
