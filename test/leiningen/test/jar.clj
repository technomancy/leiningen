(ns leiningen.test.jar
  (:require [clojure.test :refer :all]
            [leiningen.jar :refer :all]
            [clojure.java.io :as io]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [leiningen.core.utils :refer [platform-nullsink]]
            [leiningen.test.helper :as helper]
            [robert.hooke :as hooke]
            [leiningen.javac :as javac]
            [leiningen.test.helper :refer [unmemoize
                                           with-system-out-str
                                           with-system-err-str]]))

(def long-line
  (apply str (repeat 10000 "a")))

(def mock-project-1
  {:name "mock-project"
   :group "mock-group"
   :version "1.0"
   :main 'foo.one-two.three-four.bar
   :manifest [["hello" "world"]
              [:my-section-1 {"C" "D" "S" "T"}]
              ["A" "B"]
              [:my-section-2 [["E" "F"] ["X" "Y"]] ]
              ["G" "H"]
              ["long-line" long-line]]})

(def mock-project-2
  {:name "mock-project"
   :group "mock-group"
   :version "1.0"
   :main 'foo.one-two.three-four.bar
   :manifest {"hello" "world"
              :my-section-1 {"C" "D" "S" "T"}
              "A" "B"
              :my-section-2 [["E" "F"] ["X" "Y"]]
              "G" "H"
              "long-line" long-line}})

(deftest has-whitelist
  (is (= whitelist-keys project/whitelist-keys)))

(deftest test-manifest
  (doseq [mock-project [mock-project-1 mock-project-2]]
    (let [mm (-> mock-project
                 make-manifest
                 manifest-map)]
      (is (= {"Main-Class" "foo.one_two.three_four.bar", "hello" "world"}
             (select-keys mm ["hello" "Main-Class"])))
      (is (= #{"A" "Build-Jdk" "Built-By" "Created-By" "G"
               "Leiningen-Project-ArtifactId" "Leiningen-Project-GroupId"
               "Leiningen-Project-Version" "Main-Class" "Manifest-Version"
               "hello" "long-line"}
             (-> mm keys set)))
      (is (= (get mm "Leiningen-Project-ArtifactId") "mock-project"))
      (is (= (get mm "Leiningen-Project-GroupId") "mock-group"))
      (is (= (get mm "Leiningen-Project-Version") "1.0"))
      (is (= (get mm "long-line") long-line))
      (is (=  #{"my-section-1" "my-section-2"}
             (-> mock-project
                 make-manifest
                 .getEntries
                 keys
                 set))))))

(deftest test-jar-fails
  (binding [*err* (java.io.StringWriter.)]
    (with-system-out-str
      (with-system-err-str
        (is (thrown? Exception (jar helper/sample-failing-project)))))))

(deftest test-directory-entries-added-to-jar
  (with-out-str
    (let [jar (first (vals (jar helper/with-resources-project)))
          entry-names (set (helper/walkzip jar #(.getName %)))]
      (is (entry-names "nested/dir/"))
      (is (not (some #(.startsWith % "/") entry-names))))))

(deftest test-profile-added-to-jar
  (with-out-str
    (let [project (-> helper/with-resources-project
                      (project/add-profiles
                       {:test-jar {:resource-paths ^:replace []}})
                      (project/merge-profiles [:test-jar]))
          jar (first (vals (jar project)))
          entry-names (set (helper/walkzip jar #(.getName %)))]
      (is (not (entry-names "nested/dir/sample.txt"))))))

(deftest test-no-aot-jar-succeeds
  (with-out-str
    (let [project (helper/read-test-project "sample-no-aot")
          jar (first (vals (jar project)))
          entry-names (set (helper/walkzip jar #(.getName %)))]
      (is (not (entry-names "dev.clj"))))))

(deftest test-classifier-jar-succeeds
  (is (= 1 (count (:classifiers helper/with-classifiers-project)))
      "test project has a classifier")
  (with-out-str
    (is (= 1 (count (classifier-jars helper/with-classifiers-project nil)))
        "test project produces a classifier jar"))
  (with-out-str
    (is (jar helper/with-classifiers-project)
        "jar runs correctly")
    (is (= 2 (count (jar helper/with-classifiers-project)))
        "jar produces two jar files")))

(deftest ^:online test-no-deps-jar
  (unmemoize #'leiningen.core.classpath/get-dependencies-memoized
             #'leiningen.core.classpath/get-dependencies*)
  (binding [main/*info* false]
    (let [project (helper/read-test-project "sample-bad-user")
          [coord jar-file] (first (jar (dissoc project :dependencies :main)))]
      (is (.exists (io/file jar-file)))
      (is (= coord [:extension "jar"])))))

(deftest overlapped-paths
  (let [info-logs (atom [])]
    (with-redefs [main/info (fn [& args] (swap! info-logs conj args))]
      (let [result (jar helper/overlapped-sourcepaths-project)]
        (is result)
        (is (not-any? #(re-find #"Warning" %) (mapcat identity @info-logs)))))))

(def mock-project mock-project-1)

(deftest test-write-jar
  (testing (str "Confirm that a warning is output when the Main-Class is not "
                "part of the output jar file")
    (let [out-str (with-out-str
                    (write-jar mock-project
                               "/dev/null"
                               [{:type :bytes
                                 :path "foo/one_two.class"
                                 :bytes ""}
                                {:type :bytes
                                 :path "foo/one_two_too.class"
                                 :bytes ""}]))]
      (is (.contains out-str
                     "Warning: The Main-Class specified does not exist"))))

  (testing (str "Confirm that a warning is NOT output when the Main-Class is "
                "not part of the output jar file")
    (let [out-str (with-out-str
                    (write-jar mock-project
                               "/dev/null"
                               [{:type :bytes
                                 :path "foo/one_two.class"
                                 :bytes ""}
                                {:type :bytes
                                 :path "foo/one_two_too.class"
                                 :bytes ""}
                                {:type :bytes
                                 :path "foo/one_two/three_four/bar.class"
                                 :bytes ""}]))]
      (is (not (.contains out-str
                          "Warning: The Main-Class specified does not exist"))))))

(deftest javac-launched-with-whitelisted-settings
  (let [user-profile {:local-repo "test_projects/jar/foo/bar"
                      :mirrors {"central" {:name "central"
                                           :url "https://maven-central.storage-download.googleapis.com/repos/central/data/"}}}
        orig-project (-> (helper/read-test-project-with-user-profiles
                          "java-main"
                          {:user user-profile}))
        javac-project (atom {})
        javac-project-hook (fn [f proj profile]
                             (let [new-project (f proj profile)]
                               (reset! javac-project new-project)
                               new-project))]
    (hooke/with-scope
      (hooke/add-hook #'javac/javac-project-for-subprocess javac-project-hook)
      (with-out-str
        (binding [main/*info* false]
          (jar orig-project)))
      (is (= (:local-repo orig-project)
             (:local-repo @javac-project)))
      (is (= (:mirrors orig-project)
             (:mirrors @javac-project))))))
