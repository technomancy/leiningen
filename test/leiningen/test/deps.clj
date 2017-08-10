(ns leiningen.test.deps
  (:use [clojure.test]
        [leiningen.deps]
        [leiningen.test.helper :only [sample-project m2-dir m2-file native-project
                                      managed-deps-project
                                      delete-file-recursively]])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.utils :as utils]
            [leiningen.core.eval :as eval]
            [leiningen.core.classpath :as classpath]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.project :as project]))

(deftest ^:online test-deps
  (let [sample-deps [["rome" "0.9"] ["jdom" "1.0"]]]
    (doseq [[n v] sample-deps]
      (delete-file-recursively (m2-dir n v) :silently))
    ;; For some reason running deps on a project that includes ring
    ;; fails, but only when done right here. http://p.hagelb.org/mystery.gif
    (deps (update-in sample-project [:dependencies] rest))
    (doseq [[n v] sample-deps]
      (is (.exists (m2-dir n v)) (str n " was not downloaded.")))))

(defn- includes-in-order? [s substrs]
  (->> substrs
       (map #(str/index-of s %))
       (apply <)))

(deftest ^:online test-dependency-hierarchy
  (let [sample-deps [["ring" "1.0.0"] ["rome" "0.9"] ["jdom" "1.0"]]]
    (doseq [[n v] sample-deps]
      (delete-file-recursively (m2-dir n v) :silently))
    (let [out (with-out-str (deps sample-project ":tree"))
          deps '[[org.clojure/clojure "1.3.0"]
                 [rome "0.9"]
                 [jdom "1.0"]
                 [ring "1.0.0"]
                 [ring/ring-core "1.0.0"]
                 [commons-codec "1.4"]
                 [commons-io "1.4"]
                 [commons-fileupload "1.2.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [ring/ring-devel "1.0.0"]
                 [hiccup "0.3.7"]
                 [clj-stacktrace "0.2.2"]
                 [ns-tracker "0.1.1"]
                 [org.clojure/tools.namespace "0.1.0"]
                 [org.clojure/java.classpath "0.1.0"]
                 [ring/ring-jetty-adapter "1.0.0"]
                 [org.mortbay.jetty/jetty "6.1.25"]
                 [org.mortbay.jetty/servlet-api "2.5-20081211"]
                 [org.mortbay.jetty/jetty-util "6.1.25"]
                 [ring/ring-servlet "1.0.0"]]]
      (is (includes-in-order? out (map pr-str deps))))))

(deftest ^:online test-plugin-dependency-hierarchy
  (let [sample-plugin-deps [["codox" "0.6.4"]]]
    (doseq [[n v] sample-plugin-deps]
      (delete-file-recursively (m2-dir n v) :silently))
    (let [out (with-out-str (deps sample-project ":plugin-tree"))
          plugin-deps '[[codox "0.6.4"]
                        [codox/codox.leiningen "0.6.4"]
                        [leinjacker "0.4.1"]
                        [org.clojure/core.contracts "0.0.1"]
                        [org.clojure/core.unify "0.5.3"]
                        [org.clojure/clojure "1.4.0"]]]
      (is (includes-in-order? out (map pr-str plugin-deps))))))

(deftest ^:online test-snapshots-releases
  (let [pr (assoc sample-project
                  :repositories ^:replace {"clojars" {:url "https://clojars.org/repo/"
                                            :snapshots false}})
        ps (assoc sample-project
                  :repositories ^:replace {"clojars" {:url "https://clojars.org/repo/"
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

(defn coordinates-match?
  [dep1 dep2]
  ;; NOTE: there is a new function in the 0.3.1 release of pomegranate that
  ;;  is useful here, but it is private.  Calling it via the symbol dereference
  ;;  for now, but might consider making it public upstream.  Haven't done so
  ;;  yet since it is only used for tests.
  (#'aether/coordinates-match? dep1 dep2))

(deftest ^:online test-managed-deps
  (let [is-clojure-dep? #(#{'org.clojure/clojure
                            'org.clojure/tools.nrepl}
                          (first %))
        remove-clojure-deps #(remove is-clojure-dep? %)
        managed-deps (remove-clojure-deps (:managed-dependencies managed-deps-project))
        ;; find deps from normal "deps" section which explicitly specify their
        ;; version number rather than inheriting it from managed-deps
        versioned-unmanaged-deps (filter
                                  (fn [dep]
                                    (and (> (count dep) 1)
                                         (string? (nth dep 1))
                                         (not (is-clojure-dep? dep))))
                                  (:dependencies managed-deps-project))
        ;; the list of final, used deps w/versions
        merged-deps (remove-clojure-deps
                     (classpath/merge-versions-from-managed-coords
                      (:dependencies managed-deps-project)
                      (:managed-dependencies managed-deps-project)))
        ;; the list of deps from the managed deps section that aren't used
        unused-managed-deps (-> (remove
                                 (fn [dep]
                                   (or (some (partial coordinates-match? dep) merged-deps)
                                       ;; special-casing to remove tools.reader, which is a common transitive dep
                                       ;; of two of our normal dependencies
                                       (= 'org.clojure/tools.reader (first dep))))
                                 managed-deps))
        ;; deps that have classifiers
        classified-deps (filter
                         #(some #{:classifier} %)
                         merged-deps)]
    ;; make sure the sample data has some unmanaged deps, some unused managed deps,
    ;; and some classified deps, for completeness
    (is (seq versioned-unmanaged-deps))
    (is (seq unused-managed-deps))
    (is (seq classified-deps))
    ;; delete all of the existing artifacts for merged deps
    (doseq [[n v] merged-deps]
        (delete-file-recursively (m2-dir n v) :silently))
    ;; delete all of the artifacts for the managed deps too
    (doseq [[n v] managed-deps]
      (delete-file-recursively (m2-dir n v) :silently))
    ;; delete all copies of tools.reader so we know that the managed dependency
    ;; for it is taking precedence
    (delete-file-recursively (m2-dir 'org.clojure/tools.reader) :silently)
    (deps managed-deps-project)
    ;; artifacts should be available for all merged deps
    (doseq [[n v] merged-deps]
      (is (.exists (m2-dir n v)) (str n " was not downloaded (missing dir '" (m2-dir n v) "').")))
    ;; artifacts should *not* have been downloaded for unused managed deps
    (doseq [[n v] unused-managed-deps]
      (is (not (.exists (m2-dir n v))) (str n " was unexpectedly downloaded (found unexpected dir '" (m2-dir n v) "').")))
    ;; artifacts with classifiers should be available
    (doseq [[n v _ classifier] classified-deps]
      (let [f (m2-file n v classifier)]
        (is (.exists f) (str f " was not downloaded."))))
    ;; check tools.reader explicitly, since it is our special transitive dependency
    (let [tools-reader-versions (into [] (.listFiles (m2-dir 'org.clojure/tools.reader)))]
      (is (= 1 (count tools-reader-versions)))
      (is (= (first tools-reader-versions) (m2-dir 'org.clojure/tools.reader
                                                   (->> managed-deps
                                                        (filter
                                                         (fn [dep] (= 'org.clojure/tools.reader (first dep))))
                                                        first
                                                        second)))))))

(deftest test-managed-deps-with-profiles
  (testing "Able to resolve deps when profile omits versions in deps"
    (deps (project/set-profiles managed-deps-project [:add-deps])))
  (testing "Able to resolve deps when profile with ^:replace omits versions in deps"
    (deps (project/set-profiles managed-deps-project [:replace-deps]))))
