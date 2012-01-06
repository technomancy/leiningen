(ns leiningen.test.deps
  (:use [clojure.test]
        [leiningen.deps]
        [leiningen.test.helper :only [sample-project m2-dir native-project
                                      delete-file-recursively]])
  (:require [clojure.java.io :as io]
            [leiningen.main :as main]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.eval :as eval]))

(defn lib-populated? [project re]
  (some #(re-find re (.getName %))
        (file-seq (io/file (:library-path project)))))

;; Problem here is we don't get a full list of jar files back from
;; pomegranate, it looks like we just get what was actually downloaded
;; afresh. We will need the former, so need to figure this out.
(deftest test-deps
  (let [deps-files (deps sample-project)
        jars (set (map #(.getName %) deps-files ))]
    (doseq [j ["jdom-1.0.jar" "tagsoup-1.2.jar" "rome-0.9.jar"]]
      (is (jars j) (format "deps did not include %s: %s" j jars)))))

(deftest test-dev-deps-only
  (deps dev-deps-project)
  (let [jars (set (map #(.getName %)
                       (.listFiles (io/file (:root dev-deps-project)
                                            "lib" "dev"))))]
    (is (contains? jars "clojure-1.2.0.jar"))))

(deftest test-snapshots-releases
  (let [pr (assoc sample-project :omit-default-repositories true
                  :repositories {"clojars" {:url "http://clojars.org/repo/"
                                            :snapshots false}})
        ps (assoc sample-project :omit-default-repositories true
                  :repositories {"clojars" {:url "http://clojars.org/repo/"
                                            :releases false}})
        slamhound ['slamhound "1.1.0-SNAPSHOT"]
        hooke ['robert/hooke "1.0.1"]
        deps (fn [project]
               (delete-file-recursively (apply m2-dir slamhound) :quiet)
               (delete-file-recursively (apply m2-dir hooke) :quiet)
               (leiningen.deps/deps project))]
    (deps (assoc pr :dependencies [hooke]))
    (is (lib-populated? ps #"hooke"))
    (deps (assoc ps :dependencies [slamhound]))
    (is (lib-populated? ps #"slamhound"))
    (let [snaps-repo-rel-dep (assoc ps :dependencies [hooke])]
      (is (thrown? Exception (deps snaps-repo-rel-dep))))
    (let [rel-repo-snaps-dep (assoc pr :dependencies [slamhound])]
      (is (thrown? Exception (deps rel-repo-snaps-dep))))))

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
               :x86_64 #{"libjri.jnilib" "libjinput-osx.jnilib" "liblwjgl.jnilib"
                         "openal.dylib" "librxtxSerial.jnilib"
                         "libjtokyocabinet.1.1.0.dylib" "libjtokyocabinet.1.dylib"
                         "libjtokyocabinet.dylib" "libjtokyocabinet.jnilib"
                         "libtokyocabinet.9.10.0.dylib"
                         "libtokyocabinet.9.8.0.dylib" "libtokyocabinet.9.dylib"
                         "libtokyocabinet.a" "libtokyocabinet.dylib"}}
      :windows {:x86 #{"jri.dll" "rJava.dll" "jinput-dx8.dll" "jinput-raw.dll"
                       "lwjgl.dll" "OpenAL32.dll" "rxtxSerial.dll"}
                :x86_64 #{"jri.dll rJava.dll" "jinput-dx8_64.dll"
                          "jinput-raw_64.dll" "lwjgl64.dll" "OpenAL64.dll"
                          "rxtxSerial.dll"}}
      :solaris {:x86 #{"liblwjgl.so" "libopenal.so"}
                :x86_64 #{"liblwjgl64.so" "libopenal.so"}}})

(deftest test-native-deps
  (delete-file-recursively (:native-path native-project) true)
  (deps native-project)
  (is (= (conj (get-in native-lib-files-map [(eval/get-os) (eval/get-arch)])
               ".gitkeep")
         (set (for [f (rest (file-seq (eval/native-arch-path native-project)))]
                (.getName f))))))

(deftest test-checksum-deps
  (deps (assoc sample-project :checksum-deps true))
  (let [deps-ran (atom false)]
    (with-redefs [classpath/resolve-dependencies (fn [& _] (reset! deps-ran true))]
      (deps (assoc sample-project :checksum-deps true))
      (is (not @deps-ran)))))

(deftest test-explicit-checksum-deps
  (deps (assoc sample-project :checksum-deps true))
  (let [deps-ran (atom false)]
    (with-redefs [classpath/resolve-dependencies (fn [& _] (reset! deps-ran true))]
      (main/apply-task "deps" (assoc sample-project :checksum-deps true) [])
      (is @deps-ran))))
