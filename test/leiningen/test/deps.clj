(ns leiningen.test.deps
  (:use [leiningen.core :only [read-project defproject]]
        [leiningen.deps :only [deps]]
        [clojure.test]
        [clojure.java.io :only [file]]
        [leiningen.util.file :only [delete-file-recursively]]
        [leiningen.test.helper :only [sample-project dev-deps-project
                                      m2-dir with-no-log native-project]]))

(defn lib-populated? [project re]
  (some #(re-find re (.getName %))
        (file-seq (file (:root project) "lib"))))

(deftest test-deps
  (delete-file-recursively (file (:root sample-project) "lib") true)
  (deps sample-project)
  (let [jars (set (map #(.getName %)
                       (.listFiles (file (:root sample-project) "lib"))))]
    (doseq [j ["jdom-1.0.jar" "tagsoup-1.2.jar" "rome-0.9.jar"]]
      (is (jars j)))))

(deftest test-dev-deps-only
  (delete-file-recursively (file (:root dev-deps-project) "lib") true)
  (deps dev-deps-project)
  (let [jars (set (map #(.getName %)
                       (.listFiles (file (:root dev-deps-project)
                                         "lib" "dev"))))]
    (is (contains? jars "clojure-1.2.0.jar"))))

(deftest test-snapshots-releases
  (try
    (let [pr (assoc sample-project :omit-default-repositories true
                    :repositories {"clojars" {:url "http://clojars.org/repo/"
                                              :snapshots false}})
          ps (assoc sample-project :omit-default-repositories true
                    :repositories {"clojars" {:url "http://clojars.org/repo/"
                                              :releases false}})
          clj-time ['clj-time "0.3.0-SNAPSHOT"]
          hooke ['robert/hooke "1.0.1"]
          deps deps #_(fn [project]
                        (delete-file-recursively (apply m2-dir clj-time) :quiet)
                        (delete-file-recursively (apply m2-dir hooke) :quiet)
                        (leiningen.deps/deps project))]
      (deps (assoc pr :dependencies [hooke]))
      (is (lib-populated? ps #"hooke"))
      (deps (assoc ps :dependencies [clj-time]))
      (is (lib-populated? ps #"clj-time"))
      (let [snaps-repo-rel-dep (assoc ps :dependencies [hooke])]
        (is (thrown? Exception (with-no-log (deps snaps-repo-rel-dep)))))
      (let [rel-repo-snaps-dep (assoc pr :dependencies [clj-time])]
        (is (thrown? Exception (with-no-log (deps rel-repo-snaps-dep))))))
    (finally
     ;; Without triggering the GC, joda jar cannot be deleted on
     ;; Windows, which causes all sorts of seemingly unrelated test
     ;; failures. If anybody knows how to fix this properly, please do
     ;; it.
     (System/gc)
     (delete-file-recursively (file (:root sample-project) "lib")))))

(deftest test-native-deps
  (delete-file-recursively (:library-path native-project) true)
  (delete-file-recursively (:native-path native-project) true)
  (deps native-project)
  (is (= #{"liblwjgl.so" "libopenal.so" "libjtokyocabinet.so"
           "libtokyocabinet.so.9" "libjtokyocabinet.so.1" "libtokyocabinet.so"
           "libjinput-linux.so" "libtokyocabinet.so.9.8.0" ".gitkeep"
           "libtokyocabinet.so.9.10.0" "librxtxSerial.so" "libtokyocabinet.a"
           "libjri.so" "libjtokyocabinet.so.1.1.0"}
         (set (for [f (rest (file-seq (file (:native-path native-project))))]
                (.getName f))))))
