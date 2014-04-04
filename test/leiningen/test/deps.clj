(ns leiningen.test.deps
  (:use [clojure.test]
        [leiningen.deps]
        [leiningen.test.helper :only [sample-project m2-dir native-project
                                      delete-file-recursively]])
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as main]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.utils :as utils]
            [leiningen.core.eval :as eval]))

(deftest ^:online test-deps
  (let [sample-deps [["rome" "0.9"] ["jdom" "1.0"]]]
    (doseq [[n v] sample-deps]
      (delete-file-recursively (m2-dir n v) :silently))
    ;; For some reason running deps on a project that includes ring
    ;; fails, but only when done right here. http://p.hagelb.org/mystery.gif
    (deps (update-in sample-project [:dependencies] rest))
    (doseq [[n v] sample-deps]
      (is (.exists (m2-dir n v)) (str n " was not downloaded.")))))

(deftest ^:online test-dependency-hierarchy
  (let [sample-deps [["ring" "1.0.0"] ["rome" "0.9"] ["jdom" "1.0"]]]
    (doseq [[n v] sample-deps]
      (delete-file-recursively (m2-dir n v) :silently))
    (let [out (with-out-str (deps sample-project ":tree"))]
      (doseq [dep '[[org.clojure/clojure "1.3.0"]
                    [ring "1.0.0"]
                    [ring/ring-core "1.0.0"]
                    [commons-codec "1.4"]
                    [commons-fileupload "1.2.1"]
                    [commons-io "1.4"]
                    [javax.servlet/servlet-api "2.5"]
                    [ring/ring-devel "1.0.0"]
                    [clj-stacktrace "0.2.2"]
                    [hiccup "0.3.7"]
                    [ns-tracker "0.1.1"]
                    [org.clojure/tools.namespace "0.1.0"]
                    [org.clojure/java.classpath "0.1.0"]
                    [ring/ring-jetty-adapter "1.0.0"]
                    [org.mortbay.jetty/jetty-util "6.1.25"]
                    [org.mortbay.jetty/jetty "6.1.25"]
                    [org.mortbay.jetty/servlet-api "2.5-20081211"]
                    [ring/ring-servlet "1.0.0"]
                    [rome "0.9"]
                    [jdom "1.0"]]]
        (is (.contains out (pr-str dep)))))))

(deftest ^:online test-plugin-dependency-hierarchy
  (let [sample-plugin-deps [["codox" "0.6.4"]]]
    (doseq [[n v] sample-plugin-deps]
      (delete-file-recursively (m2-dir n v) :silently))
    (let [out (with-out-str (deps sample-project ":plugin-tree"))]
      (doseq [plugin-dep '[[codox "0.6.4"]
                           [codox/codox.leiningen "0.6.4"]
                           [leinjacker "0.4.1"]
                           [org.clojure/core.contracts "0.0.1"]
                           [org.clojure/clojure "1.4.0"]
                           [org.clojure/core.unify "0.5.3"]]]
        (is (.contains out (pr-str plugin-dep)))))))

(deftest ^:online test-snapshots-releases
  (let [pr (assoc sample-project
                  :repositories ^:replace {"clojars" {:url "http://clojars.org/repo/"
                                            :snapshots false}})
        ps (assoc sample-project
                  :repositories ^:replace {"clojars" {:url "http://clojars.org/repo/"
                                            :releases false}})
        slamhound ['slamhound "1.1.0-SNAPSHOT"]
        hooke ['robert/hooke "1.0.1"]
        deps (fn [project]
               (delete-file-recursively (apply m2-dir slamhound) :quiet)
               (delete-file-recursively (apply m2-dir hooke) :quiet)
               (leiningen.deps/deps project))]
    (deps (assoc pr :dependencies [hooke]))
    (is (.exists (m2-dir :robert/hooke "1.0.1")))
    (deps (assoc ps :dependencies [slamhound]))
    (is (.exists (m2-dir "slamhound" "1.1.0-SNAPSHOT")))
    (let [snaps-repo-rel-dep (assoc ps :dependencies [hooke])]
      (is (thrown? Exception (deps snaps-repo-rel-dep)))
      (is (not (.exists (m2-dir :robert/hooke "1.0.1")))))
    (let [rel-repo-snaps-dep (assoc pr :dependencies [slamhound])]
      (is (thrown? Exception (deps rel-repo-snaps-dep)))
      (is (not (.exists (m2-dir "slamhound" "1.1.0-SNAPSHOT"))))) ))

(def native-lib-files-map
  {:linux {:x86 #{"libjri.so" "libjinput-linux.so" "liblwjgl.so" "libopenal.so"
                  "librxtxSerial.so" "libjtokyocabinet.so"
                  "libjtokyocabinet.so.1" "libjtokyocabinet.so.1.1.0"
                  "libtokyocabinet.a" "libtokyocabinet.so"
                  "libtokyocabinet.so.9" "libtokyocabinet.so.9.10.0"
                  "libtokyocabinet.so.9.8.0"}
           :x86_64 #{"libjri.so" "libjinput-linux64.so" "liblwjgl64.so"
                     "libopenal64.so" "librxtxSerial.so" "libjtokyocabinet.so"
                     "libjtokyocabinet.so.1" "libjtokyocabinet.so.1.1.0"
                     "libtokyocabinet.a" "libtokyocabinet.so"
                     "libtokyocabinet.so.9" "libtokyocabinet.so.9.10.0"
                     "libtokyocabinet.so.9.8.0"}}
   :macosx {:x86 #{"libjri.jnilib" "libjinput-osx.jnilib" "liblwjgl.jnilib"
                   "openal.dylib" "librxtxSerial.jnilib"
                   "libjtokyocabinet.1.1.0.dylib" "libjtokyocabinet.1.dylib"
                   "libjtokyocabinet.dylib" "libjtokyocabinet.jnilib"
                   "libtokyocabinet.9.10.0.dylib"
                   "libtokyocabinet.9.8.0.dylib" "libtokyocabinet.9.dylib"
                   "libtokyocabinet.a" "libtokyocabinet.dylib"}
            :x86_64 #{"libjri.jnilib" "libjinput-osx.jnilib"
                      "liblwjgl.jnilib"
                      "openal.dylib" "librxtxSerial.jnilib"
                      "libjtokyocabinet.1.1.0.dylib"
                      "libjtokyocabinet.1.dylib"
                      "libjtokyocabinet.dylib" "libjtokyocabinet.jnilib"
                      "libtokyocabinet.9.10.0.dylib"
                      "libtokyocabinet.9.8.0.dylib" "libtokyocabinet.9.dylib"
                      "libtokyocabinet.a" "libtokyocabinet.dylib"}}
   :windows {:x86 #{"jri.dll" "rJava.dll" "jinput-dx8.dll" "jinput-raw.dll"
                    "lwjgl.dll" "OpenAL32.dll" "rxtxSerial.dll"}
             :x86_64 #{"jri.dll" "rJava.dll" "jinput-dx8_64.dll"
                       "jinput-raw_64.dll" "lwjgl64.dll" "OpenAL64.dll"
                       "rxtxSerial.dll"}}
   :solaris {:x86 #{"liblwjgl.so" "libopenal.so"}
             :x86_64 #{"liblwjgl64.so" "libopenal.so"}}})

(deftest test-native-deps
  (delete-file-recursively (:target-path native-project) true)
  (deps native-project)
  (is (= (conj (get-in native-lib-files-map [(utils/get-os) (utils/get-arch)])
               ".gitkeep")
         (set (for [f (rest (file-seq (io/file (first (eval/native-arch-paths
                                                       native-project)))))]
                (.getName f))))))
