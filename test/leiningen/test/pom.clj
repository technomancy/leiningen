(ns leiningen.test.pom
  (:use [clojure.test]
        [clojure.java.io :only [file delete-file]]
        [leiningen.pom :as pom :only [make-pom pom snapshot?]]
        [leiningen.core.user :as user]
        [leiningen.test.helper
         :only [sample-project sample-profile-meta-project
                managed-deps-project managed-deps-snapshot-project]
         :as lthelper])
  (:require [clojure.data.xml :as xml]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]))

(use-fixtures :once (fn [f]
                      (with-redefs [user/profiles (constantly {})]
                        (f))))

(deftest test-pom-file-is-created
  (let [pom-file (file (:root sample-project) "pom.xml")]
    (delete-file pom-file true)
    (pom sample-project)
    (is (.exists pom-file))))

(defn deep-content [xml tags]
  (reduce #(->> %1
               (filter (fn [xml] (= (:tag xml) %2)))
               first
               :content)
          (if (seq? xml)
            xml
            [xml])
          tags))

(def first-in (comp first deep-content))

(defn with-profile [project name profile]
  (let [profile (#'project/apply-profile-meta
                 (project/default-profile-metadata name)
                 profile)]
    (-> project
        (vary-meta update-in [:without-profiles :profiles]
                   assoc name profile)
        (vary-meta update-in [:profiles]
                   assoc name profile))))

(defn with-profile-merged
  ([project profile]
     (with-profile-merged project :testy profile))
  ([project name profile]
      (project/merge-profiles (with-profile project name profile) [name])))

(deftest test-pom-scm-auto
  (with-redefs [pom/parse-github-url (constantly ["techno" "lein"])
                pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "auto"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection "https://example.org/ignored-url"
                                 :url "https://github.com/this-is/ignored"}})
        pom (make-pom project)
        xml (xml/parse-str pom)]
      (is (= "scm:git:git://github.com/techno/lein.git" (first-in xml [:project :scm :connection])))
      (is (= "scm:git:ssh://git@github.com/techno/lein.git" (first-in xml [:project :scm :developerConnection])))
      (is (= "https://github.com/techno/lein" (first-in xml [:project :scm :url])))
      (is (= "the git head" (first-in xml [:project :scm :tag]))))))

(deftest test-pom-scm-git
  (with-redefs [pom/parse-github-url (constantly ["techno" "lein"])
                pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "git"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection ":connection is not ignored in :scm :git"
                                 :url "https://github.com/this-is-not/ignored"}})
        pom (make-pom project)
        xml (xml/parse-str pom)]
      (is (= ":connection is not ignored in :scm :git" (first-in xml [:project :scm :connection])))
      (is (= "scm:git:ssh://git@github.com/techno/lein.git" (first-in xml [:project :scm :developerConnection])))
      (is (= "https://github.com/this-is-not/ignored" (first-in xml [:project :scm :url])))
      (is (= "the git head" (first-in xml [:project :scm :tag]))))))

(deftest test-pom-scm-git-with-empty-values
  (with-redefs [pom/parse-github-url (constantly ["techno" "lein"])
                pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "git"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection ""
                                 :developerConnection nil
                                 :url "https://github.com/this-is-not/ignored"}})
        pom (make-pom project)
        xml (xml/parse-str pom)]
      (is (nil? (first-in xml [:project :scm :connection]))
          ":connection is not present because the project defines an empty value for it")
      (is (nil? (first-in xml [:project :scm :developerConnection]))
          ":developerConnection is not present because the project defines an empty value for it")
      (is (= "https://github.com/this-is-not/ignored" (first-in xml [:project :scm :url])))
      (is (= "the git head" (first-in xml [:project :scm :tag]))))))

(deftest test-pom-default-values
  (let [xml (xml/parse-str (make-pom sample-project))]
    (is (= "nomnomnom" (first-in xml [:project :groupId]))
        "group is correct")
    (is (= "nomnomnom" (first-in xml [:project :artifactId]))
        "artifact is correct")
    (is (= "nomnomnom" (first-in xml [:project :name]))
        "name is correct")
    (is (= "0.5.0-SNAPSHOT" (first-in xml [:project :version]))
        "version is correct")
    (is (nil? (first-in xml [:project :parent]))
        "no parent")
    (is (= "http://leiningen.org" (first-in xml [:project :url]))
        "url is correct")
    (is (= ["Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"]
           (->> (deep-content xml [:project :licenses])
                (map :content) first (map :content) (map first)))
        "no license")
    (is (= "A test project" (first-in xml [:project :description]))
        "description is included")
    (is (= nil (first-in xml [:project :mailingLists]))
        "no mailing list")
    (is (= ["central" "clojars" "other"]
           (map #(first-in % [:repository :id])
                (deep-content xml [:project :repositories])))
        "repositories are named")
    (is (= ["https://repo1.maven.org/maven2/" "https://repo.clojars.org/"
            "http://example.com/repo"]
           (map #(first-in % [:repository :url])
                (deep-content xml [:project :repositories])))
        "repositories have correct location")
    (is (= ["false" "true" "true"]
           (map #(first-in % [:repository :snapshots :enabled])
                (deep-content xml [:project :repositories])))
        "some snapshots are enabled")
    (is (= ["true" "true" "true"]
           (map #(first-in % [:repository :releases :enabled])
                (deep-content xml [:project :repositories])))
        "releases are enabled")
    (is (= "src" (first-in xml [:project :build :sourceDirectory]))
        "source directory is included")
    (is (= "test" (first-in xml [:project :build :testSourceDirectory]))
        "test directory is included")
    (is (= ["resources"]
           (map #(first-in % [:resource :directory])
                (deep-content xml [:project :build :resources])))
        "resource directories use project without :default or :dev profile")
    (is (= ["resources"]
           (map #(first-in % [:testResource :directory])
                (deep-content xml [:project :build :testResources])))
        "test resource directories use :dev :default and :test profiles")
    (is (= "target" (first-in xml [:project :build :directory]))
        "target directory is included")
    (is (= nil (first-in xml [:project :build :extensions]))
        "no extensions")
    (is (= (lthelper/fix-path-delimiters "target/classes") (first-in xml [:project :build :outputDirectory]))
        "classes directory is included")
    (is (= ["org.clojure" "rome" "ring"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))))

(deftest test-dependencies-are-test-scoped
  (let [xml (xml/parse-str
             (make-pom (with-profile-merged
                         sample-project
                         :test {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "test"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))))

(deftest dev-dependencies-are-test-scoped
  (let [xml (xml/parse-str
             (make-pom (with-profile-merged
                         sample-project
                         :dev
                         {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "test"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))))

(deftest provided-dependencies-are-provided-scoped
  (let [xml (xml/parse-str
             (make-pom (with-profile-merged
                         sample-project
                         :provided
                         {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "provided"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))))

(deftest dependency-options
  (let [xml (xml/parse-str
             (make-pom (with-profile-merged
                         sample-project
                         ^:leaky {:dependencies '[[peridot "0.0.5"
                                                   :scope "provided"
                                                   :optional true
                                                   :classifier "sources"
                                                   :extension "pom"
                                                   :exclusions
                                                   [[ring-mock
                                                     :classifier "cla"
                                                     :extension "dom"]]]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "provided"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "true"]
           (map #(first-in % [:dependency :optional])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "sources"]
           (map #(first-in % [:dependency :classifier])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "pom"]
           (map #(first-in % [:dependency :type])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "ring-mock"]
           (map #(first-in % [:dependency :exclusions :exclusion :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "ring-mock"]
           (map #(first-in % [:dependency :exclusions :exclusion :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "cla"]
           (map #(first-in % [:dependency :exclusions :exclusion :classifier])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "dom"]
           (map #(first-in % [:dependency :exclusions :exclusion :type])
                (deep-content xml [:project :dependencies])))))
  (let [xml (xml/parse-str
             (make-pom (with-profile-merged
                         sample-project
                         ^:leaky {:dependencies '[[peridot "0.0.5"
                                                   :scope "provided"
                                                   :exclusions
                                                   [ring-mock]]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "provided"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "ring-mock"]
           (map #(first-in % [:dependency :exclusions :exclusion :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :classifier])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil  nil]
           (map #(first-in % [:dependency :exclusions :exclusion :type])
                (deep-content xml [:project :dependencies]))))))

(deftest dependencies-are-required-when-overlapped-by-builtin-profiles
  (let [xml (xml/parse-str
             (make-pom (with-profile-merged
                         sample-project
                         :dev {:dependencies '[[rome "0.8"]]})))]
    (is (= ["org.clojure" "rome" "ring"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.3.0" "0.8" "1.0.0"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil "test" nil]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))))

(deftest test-pom-has-classifier-when-defined
  (is (not (re-find #"classifier"
                    (make-pom sample-project))))
  (is (= "stuff"
         (-> (make-pom (with-profile-merged
                         sample-project
                         ^:leaky {:classifier "stuff"}))
              xml/parse-str
              (first-in [:project :classifier])))))

(deftest test-pom-adds-java-source-paths
  (is (= (vec (map lthelper/fix-path-delimiters ["java/src" "java/another"]))
         (-> (make-pom (with-profile-merged sample-project
                         ^:leaky
                         {:java-source-paths ["java/src" "java/another"]}))
             xml/parse-str
             (deep-content [:project :build :plugins :plugin :executions
                            :execution :configuration :sources])
             ((partial mapcat :content))))))

(deftest test-pom-handles-global-exclusions
  (is (= [["clojure"] ["clojure"] ["clojure"]]
         (-> (make-pom (with-profile-merged sample-project
                         ^:leaky {:exclusions '[org.clojure/clojure]}))
             xml/parse-str
             (deep-content [:project :dependencies])
             ((partial map #(deep-content % [:dependency :exclusions])))
             ((partial map
                       (partial map
                                #(first-in % [:exclusion :artifactId]))))))))

(deftest test-pom-tries-to-pprint
  (is (re-find #"(?m)^\s+<groupId>nomnomnom</groupId>$"
               (make-pom sample-project))))

(deftest test-snapshot-checking
  (binding [main/*exit-process?* false]
    (let [project (vary-meta sample-project update-in [:without-profiles] assoc
                             :version "1.0"
                             :dependencies [['clojure "1.0.0-SNAPSHOT"]])]
      (is (thrown? Exception (pom project))))))

(deftest test-classifier-kept
  (let [xml (xml/parse-str (make-pom lthelper/native-project))]
    (is (= [["gdx-platform" nil] ["gdx-platform" "natives-desktop"]]
           (for [dep (deep-content xml [:project :dependencies])
                 :let [artifact (first-in dep [:dependency :artifactId])]
                 :when (= "gdx-platform" artifact)]
             [artifact (first-in dep [:dependency :classifier])])))))

(deftest test-override-base-profile
  (testing "leaky explicit profile"
    (let [p (make-pom (with-profile-merged sample-project
                        ^:leaky
                        {:dependencies [['nrepl/nrepl "0.4.5"]]}))
          deps (deep-content (xml/parse-str p) [:project :dependencies])
          nrepls (filter #(re-find #"nrepl" (pr-str %)) deps)
          versions (map #(deep-content % [:dependency :version]) nrepls)]
      (is (= [["0.4.5"]] versions))))
  (testing "pom-scope"
    (let [p (make-pom (with-profile-merged sample-project
                        ^{:pom-scope :test}
                        {:dependencies [['nrepl/nrepl "0.4.5"]]}))
          deps (deep-content (xml/parse-str p) [:project :dependencies])
          nrepls (filter #(re-find #"nrepl" (pr-str %)) deps)
          versions (map #(deep-content % [:dependency :version]) nrepls)]
      (is (= [["0.4.5"]] versions)))))

(deftest test-leaky-profile
  (let [p (make-pom sample-profile-meta-project)
        deps (deep-content (xml/parse-str p) [:project :dependencies])
        t-m (filter #(re-find #"tools.macro" (pr-str %)) deps)
        j-c (filter #(re-find #"java.classpath" (pr-str %)) deps)
        t-n (filter #(re-find #"tools.namespace" (pr-str %)) deps)]
    (is (= [["0.1.2"]] (map #(deep-content % [:dependency :version]) t-m)))
    (is (= [["0.2.2"]] (map #(deep-content % [:dependency :version]) j-c)))
    (is (= [["0.2.6"]] (map #(deep-content % [:dependency :version]) t-n)))
    (is (= [nil] (map #(deep-content % [:dependency :scope]) t-m)))
    (is (= [["test"]] (map #(deep-content % [:dependency :scope]) j-c)))
    (is (= [["provided"]] (map #(deep-content % [:dependency :scope]) t-n)))))

(deftest test-determine-release-type
  (testing "Version containing SNAPSHOT is treated as snapshot"
    (is (snapshot? {:version "SNAPSHOT"}))
    (is (snapshot? {:version "fooSNAPSHOTbar"})))
  (testing "Version containing anything else is not a snapshot "
    (is (not (snapshot? {:version "foo"})))
    (is (not (snapshot? nil)))))

(deftest test-managed-dependencies
  (doseq [proj [managed-deps-snapshot-project
                managed-deps-project]]
    (let [xml (xml/parse-str
               (make-pom proj))]
      (testing "normal dependencies are written to pom properly"
        (is (= ["org.clojure" "rome" "ring" "ring" "ring" "commons-codec"
                "commons-math" "org.apache.commons" "org.clojure" "org.clojure"]
               (map #(first-in % [:dependency :groupId])
                    (deep-content xml [:project :dependencies]))))
        (is (= ["clojure" "rome" "ring" "ring-codec" "ring-headers"
                "commons-codec" "commons-math" "commons-csv"
                "tools.emitter.jvm" "tools.namespace"]
               (map #(first-in % [:dependency :artifactId])
                    (deep-content xml [:project :dependencies]))))
        (is (= [nil nil nil nil nil "1.6" nil nil "0.1.0-beta5" "0.3.0-alpha3"]
               (map #(first-in % [:dependency :version])
                    (deep-content xml [:project :dependencies])))))
      (testing "managed dependencies are written to pom properly"
        (is (= ["org.clojure" "rome" "ring" "ring" "ring" "commons-math"
                "org.apache.commons" "ring" "org.clojure"]
               (map #(first-in % [:dependency :groupId])
                    (deep-content xml [:project :dependencyManagement :dependencies]))))
        (is (= ["clojure" "rome" "ring" "ring-codec" "ring-headers"
                "commons-math" "commons-csv" "ring-defaults" "tools.reader"]
               (map #(first-in % [:dependency :artifactId])
                    (deep-content xml [:project :dependencyManagement :dependencies]))))
        (is (= ["1.3.0" "0.9" "1.0.0" "1.0.1" "0.2.0" "1.2" "1.4" "0.2.1" "1.0.0-beta3"]
               (map #(first-in % [:dependency :version])
                    (deep-content xml [:project :dependencyManagement :dependencies]))))
        (is (= [nil nil nil nil nil "sources" "sources" nil nil]
               (map #(first-in % [:dependency :classifier])
                    (deep-content xml [:project :dependencyManagement :dependencies]))))))))
