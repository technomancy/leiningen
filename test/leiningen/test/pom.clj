(ns leiningen.test.pom
  (:use [clojure.test]
        [clojure.java.io :only [file delete-file]]
        [leiningen.pom :as lein-pom :only [make-pom pom pom-uri snapshot?]]
        [leiningen.core.user :as user]
        [leiningen.test.helper
         :only [sample-project sample-profile-meta-project
                managed-deps-project managed-deps-snapshot-project
                with-pom-plugins-project]
         :as lthelper])
  (:require [clojure.data.xml :as xml]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]))

(use-fixtures :once (fn [f]
                      (with-redefs [user/profiles (constantly {})]
                        (f))))

(xml/alias-uri 'pom pom-uri)

(deftest test-pom-file-is-created
  (let [pom-file (file (:root sample-project) "pom.xml")]
    (delete-file pom-file true)
    (pom sample-project)
    (is (.exists pom-file))))

(defn parse-xml [s]
  (xml/parse-str s :skip-whitespace true))

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
  (with-redefs [lein-pom/parse-github-url (constantly ["techno" "lein"])
                lein-pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "auto"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection "https://example.org/ignored-url"
                                 :url "https://github.com/this-is/ignored"}})
        pom (make-pom project)
        xml (parse-xml pom)]
      (is (= "scm:git:git://github.com/techno/lein.git" (first-in xml [::pom/project ::pom/scm ::pom/connection])))
      (is (= "scm:git:ssh://git@github.com/techno/lein.git" (first-in xml [::pom/project ::pom/scm ::pom/developerConnection])))
      (is (= "https://github.com/techno/lein" (first-in xml [::pom/project ::pom/scm ::pom/url])))
      (is (= "the git head" (first-in xml [::pom/project ::pom/scm ::pom/tag]))))))

(deftest test-pom-scm-git
  (with-redefs [lein-pom/read-git-origin (constantly "git@github.com:techno/lein.git")
                lein-pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "git"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection ":connection is not ignored in :scm :git"
                                 :url "https://github.com/this-is-not/ignored"}})
        pom (make-pom project)
        xml (parse-xml pom)]
      (is (= ":connection is not ignored in :scm :git" (first-in xml [::pom/project ::pom/scm ::pom/connection])))
      (is (= "scm:git:ssh://git@github.com/techno/lein.git" (first-in xml [::pom/project ::pom/scm ::pom/developerConnection])))
      (is (= "https://github.com/this-is-not/ignored" (first-in xml [::pom/project ::pom/scm ::pom/url])))
      (is (= "the git head" (first-in xml [::pom/project ::pom/scm ::pom/tag]))))))

(deftest test-pom-scm-git-with-empty-values
  (with-redefs [lein-pom/parse-github-url (constantly ["techno" "lein"])
                lein-pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "git"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection ""
                                 :developerConnection nil
                                 :url "https://github.com/this-is-not/ignored"}})
        pom (make-pom project)
        xml (parse-xml pom)]
      (is (nil? (first-in xml [::pom/project ::pom/scm ::pom/connection]))
          ":connection is not present because the project defines an empty value for it")
      (is (nil? (first-in xml [::pom/project ::pom/scm ::pom/developerConnection]))
          ":developerConnection is not present because the project defines an empty value for it")
      (is (= "https://github.com/this-is-not/ignored" (first-in xml [::pom/project ::pom/scm ::pom/url])))
      (is (= "the git head" (first-in xml [::pom/project ::pom/scm ::pom/tag]))))))

(deftest test-pom-scm-git-with-https-url
  (with-redefs [lein-pom/read-git-origin (constantly "https://github.com/techno/lein.git")
                lein-pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "git"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection ":connection is not ignored in :scm :git"
                                 :url "https://github.com/this-is-not/ignored"}})
        pom (make-pom project)
        xml (parse-xml pom)]
      (is (= ":connection is not ignored in :scm :git" (first-in xml [::pom/project ::pom/scm ::pom/connection])))
      (is (= "scm:git:ssh://git@github.com/techno/lein.git" (first-in xml [::pom/project ::pom/scm ::pom/developerConnection])))
      (is (= "https://github.com/this-is-not/ignored" (first-in xml [::pom/project ::pom/scm ::pom/url])))
      (is (= "the git head" (first-in xml [::pom/project ::pom/scm ::pom/tag]))))))

(deftest test-pom-scm-git-with-non-git-url
  (with-redefs [lein-pom/read-git-origin (constantly "https://github.com/techno/lein")
                lein-pom/read-git-head (constantly "the git head")]
    (let [project (with-profile-merged sample-project
                  ^:leaky {:scm {:name "git"
                                 :dir "." ;; so resolve-git-dir looks for lein project .git dir, not the sample
                                 :connection ":connection is not ignored in :scm :git"
                                 :url "https://github.com/this-is-not/ignored"}})
        pom (make-pom project)
        xml (parse-xml pom)]
      (is (= ":connection is not ignored in :scm :git" (first-in xml [::pom/project ::pom/scm ::pom/connection])))
      (is (= "scm:git:ssh://git@github.com/techno/lein.git" (first-in xml [::pom/project ::pom/scm ::pom/developerConnection])))
      (is (= "https://github.com/this-is-not/ignored" (first-in xml [::pom/project ::pom/scm ::pom/url])))
      (is (= "the git head" (first-in xml [::pom/project ::pom/scm ::pom/tag]))))))

(deftest test-pom-default-values
  (let [xml (parse-xml (make-pom sample-project))]
    (is (= "nomnomnom" (first-in xml [::pom/project ::pom/groupId]))
        "group is correct")
    (is (= "nomnomnom" (first-in xml [::pom/project ::pom/artifactId]))
        "artifact is correct")
    (is (= "nomnomnom" (first-in xml [::pom/project ::pom/name]))
        "name is correct")
    (is (= "0.5.0-SNAPSHOT" (first-in xml [::pom/project ::pom/version]))
        "version is correct")
    (is (nil? (first-in xml [::pom/project ::pom/parent]))
        "no parent")
    (is (= "http://leiningen.org" (first-in xml [::pom/project ::pom/url]))
        "url is correct")
    (is (= ["Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"]
           (->> (deep-content xml [::pom/project ::pom/licenses])
                (map :content) first (map :content) (map first)))
        "no license")
    (is (= "A test project" (first-in xml [::pom/project ::pom/description]))
        "description is included")
    (is (= nil (first-in xml [::pom/project ::pom/mailingLists]))
        "no mailing list")
    (is (= ["central" "clojars" "other"]
           (map #(first-in % [::pom/repository ::pom/id])
                (deep-content xml [::pom/project ::pom/repositories])))
        "repositories are named")
    (is (= ["https://repo1.maven.org/maven2/" "https://repo.clojars.org/"
            "http://example.com/repo"]
           (map #(first-in % [::pom/repository ::pom/url])
                (deep-content xml [::pom/project ::pom/repositories])))
        "repositories have correct location")
    (is (= ["false" "true" "true"]
           (map #(first-in % [::pom/repository ::pom/snapshots ::pom/enabled])
                (deep-content xml [::pom/project ::pom/repositories])))
        "some snapshots are enabled")
    (is (= ["true" "true" "true"]
           (map #(first-in % [::pom/repository ::pom/releases ::pom/enabled])
                (deep-content xml [::pom/project ::pom/repositories])))
        "releases are enabled")
    (is (= [nil nil "always"]
           (map #(first-in % [::pom/repository ::pom/snapshots ::pom/updatePolicy])
                (deep-content xml [::pom/project ::pom/repositories])))
        "snapshots update policy is included")
    (is (= [nil nil "warn"]
           (map #(first-in % [::pom/repository ::pom/releases ::pom/checksumPolicy])
                (deep-content xml [::pom/project ::pom/repositories])))
        "releases checksum policy is included")
    (is (= "src" (first-in xml [::pom/project ::pom/build ::pom/sourceDirectory]))
        "source directory is included")
    (is (= "test" (first-in xml [::pom/project ::pom/build ::pom/testSourceDirectory]))
        "test directory is included")
    (is (= ["resources"]
           (map #(first-in % [::pom/resource ::pom/directory])
                (deep-content xml [::pom/project ::pom/build ::pom/resources])))
        "resource directories use project without :default or :dev profile")
    (is (= ["resources"]
           (map #(first-in % [::pom/testResource ::pom/directory])
                (deep-content xml [::pom/project ::pom/build ::pom/testResources])))
        "test resource directories use :dev :default and :test profiles")
    (is (= "target" (first-in xml [::pom/project ::pom/build ::pom/directory]))
        "target directory is included")
    (is (= nil (first-in xml [::pom/project ::pom/build ::pom/extensions]))
        "no extensions")
    (is (= (lthelper/fix-path-delimiters "target/classes")
           (first-in xml [::pom/project ::pom/build ::pom/outputDirectory]))
        "classes directory is included")
    (is (= ["org.clojure" "rome" "ring"]
           (map #(first-in % [::pom/dependency ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["clojure" "rome" "ring"]
           (map #(first-in % [::pom/dependency ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0"]
           (map #(first-in % [::pom/dependency ::pom/version])
                (deep-content xml [::pom/project ::pom/dependencies]))))))

(deftest test-dependencies-are-test-scoped
  (let [xml (parse-xml
             (make-pom (with-profile-merged
                         sample-project
                         :test {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [::pom/dependency ::pom/version])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "test"]
           (map #(first-in % [::pom/dependency ::pom/scope])
                (deep-content xml [::pom/project ::pom/dependencies]))))))

(deftest dev-dependencies-are-test-scoped
  (let [xml (parse-xml
             (make-pom (with-profile-merged
                         sample-project
                         :dev
                         {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [::pom/dependency ::pom/version])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "test"]
           (map #(first-in % [::pom/dependency ::pom/scope])
                (deep-content xml [::pom/project ::pom/dependencies]))))))

(deftest provided-dependencies-are-provided-scoped
  (let [xml (parse-xml
             (make-pom (with-profile-merged
                         sample-project
                         :provided
                         {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [::pom/dependency ::pom/version])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "provided"]
           (map #(first-in % [::pom/dependency ::pom/scope])
                (deep-content xml [::pom/project ::pom/dependencies]))))))

(deftest dependency-options
  (let [xml (parse-xml
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
           (map #(first-in % [::pom/dependency ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [::pom/dependency ::pom/version])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "provided"]
           (map #(first-in % [::pom/dependency ::pom/scope])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "true"]
           (map #(first-in % [::pom/dependency ::pom/optional])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "sources"]
           (map #(first-in % [::pom/dependency ::pom/classifier])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "pom"]
           (map #(first-in % [::pom/dependency ::pom/type])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "ring-mock"]
           (map #(first-in % [::pom/dependency ::pom/exclusions ::pom/exclusion ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "ring-mock"]
           (map #(first-in % [::pom/dependency ::pom/exclusions ::pom/exclusion ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "cla"]
           (map #(first-in % [::pom/dependency ::pom/exclusions ::pom/exclusion ::pom/classifier])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "dom"]
           (map #(first-in % [::pom/dependency ::pom/exclusions ::pom/exclusion ::pom/type])
                (deep-content xml [::pom/project ::pom/dependencies])))))
  (let [xml (parse-xml
             (make-pom (with-profile-merged
                         sample-project
                         ^:leaky {:dependencies '[[peridot "0.0.5"
                                                   :scope "provided"
                                                   :exclusions
                                                   [ring-mock]]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [::pom/dependency ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["1.3.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [::pom/dependency ::pom/version])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "provided"]
           (map #(first-in % [::pom/dependency ::pom/scope])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil "ring-mock"]
           (map #(first-in % [::pom/dependency ::pom/exclusions ::pom/exclusion ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil nil]
           (map #(first-in % [::pom/dependency ::pom/exclusions ::pom/exclusion ::pom/classifier])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil nil nil  nil]
           (map #(first-in % [::pom/dependency ::pom/exclusions ::pom/exclusion ::pom/type])
                (deep-content xml [::pom/project ::pom/dependencies]))))))

(deftest dependencies-are-required-when-overlapped-by-builtin-profiles
  (let [xml (parse-xml
             (make-pom (with-profile-merged
                         sample-project
                         :dev {:dependencies '[[rome "0.8"]]})))]
    (is (= ["org.clojure" "rome" "ring"]
           (map #(first-in % [::pom/dependency ::pom/groupId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["clojure" "rome" "ring"]
           (map #(first-in % [::pom/dependency ::pom/artifactId])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= ["1.3.0" "0.8" "1.0.0"]
           (map #(first-in % [::pom/dependency ::pom/version])
                (deep-content xml [::pom/project ::pom/dependencies]))))
    (is (= [nil "test" nil]
           (map #(first-in % [::pom/dependency ::pom/scope])
                (deep-content xml [::pom/project ::pom/dependencies]))))))

(deftest test-pom-has-classifier-when-defined
  (is (not (re-find #"classifier"
                    (make-pom sample-project))))
  (is (= "stuff"
         (-> (make-pom (with-profile-merged
                         sample-project
                         ^:leaky {:classifier "stuff"}))
              parse-xml
              (first-in [::pom/project ::pom/classifier])))))

(deftest test-pom-adds-java-source-paths
  (is (= (vec (map lthelper/fix-path-delimiters ["java/src" "java/another"]))
         (-> (make-pom (with-profile-merged sample-project
                         ^:leaky
                         {:java-source-paths ["java/src" "java/another"]}))
             parse-xml
             (deep-content [::pom/project ::pom/build ::pom/plugins ::pom/plugin ::pom/executions
                            ::pom/execution ::pom/configuration ::pom/sources])
             ((partial mapcat :content))))))

(deftest test-pom-handles-global-exclusions
  (is (= [["clojure"] ["clojure"] ["clojure"]]
         (-> (make-pom (with-profile-merged sample-project
                         ^:leaky {:exclusions '[org.clojure/clojure]}))
             parse-xml
             (deep-content [::pom/project ::pom/dependencies])
             ((partial map #(deep-content % [::pom/dependency ::pom/exclusions])))
             ((partial map
                       (partial map
                                #(first-in % [::pom/exclusion ::pom/artifactId]))))))))

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
  (let [xml (parse-xml (make-pom lthelper/native-project))]
    (is (= [["gdx-platform" nil] ["gdx-platform" "natives-desktop"]]
           (for [dep (deep-content xml [::pom/project ::pom/dependencies])
                 :let [artifact (first-in dep [::pom/dependency ::pom/artifactId])]
                 :when (= "gdx-platform" artifact)]
             [artifact (first-in dep [::pom/dependency ::pom/classifier])])))))

(deftest test-override-base-profile
  (testing "leaky explicit profile"
    (let [p (make-pom (with-profile-merged sample-project
                        ^:leaky
                        {:dependencies [['nrepl/nrepl "0.4.5"]]}))
          deps (deep-content (parse-xml p) [::pom/project ::pom/dependencies])
          nrepls (filter #(re-find #"nrepl" (pr-str %)) deps)
          versions (map #(deep-content % [::pom/dependency ::pom/version]) nrepls)]
      (is (= [["0.4.5"]] versions))))
  (testing "pom-scope"
    (let [p (make-pom (with-profile-merged sample-project
                        ^{:pom-scope :test}
                        {:dependencies [['nrepl/nrepl "0.4.5"]]}))
          deps (deep-content (parse-xml p) [::pom/project ::pom/dependencies])
          nrepls (filter #(re-find #"nrepl" (pr-str %)) deps)
          versions (map #(deep-content % [::pom/dependency ::pom/version]) nrepls)]
      (is (= [["0.4.5"]] versions)))))

(deftest test-leaky-profile
  (let [p (make-pom sample-profile-meta-project)
        deps (deep-content (parse-xml p) [::pom/project ::pom/dependencies])
        t-m (filter #(re-find #"tools.macro" (pr-str %)) deps)
        j-c (filter #(re-find #"java.classpath" (pr-str %)) deps)
        t-n (filter #(re-find #"tools.namespace" (pr-str %)) deps)]
    (is (= [["0.1.2"]] (map #(deep-content % [::pom/dependency ::pom/version]) t-m)))
    (is (= [["0.2.2"]] (map #(deep-content % [::pom/dependency ::pom/version]) j-c)))
    (is (= [["0.2.6"]] (map #(deep-content % [::pom/dependency ::pom/version]) t-n)))
    (is (= [nil] (map #(deep-content % [::pom/dependency ::pom/scope]) t-m)))
    (is (= [["test"]] (map #(deep-content % [::pom/dependency ::pom/scope]) j-c)))
    (is (= [["provided"]] (map #(deep-content % [::pom/dependency ::pom/scope]) t-n)))))

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
    (let [xml (parse-xml
               (make-pom proj))]
      (testing "normal dependencies are written to pom properly"
        (is (= ["org.clojure" "rome" "ring" "ring" "ring" "commons-codec"
                "commons-math" "org.apache.commons" "org.clojure" "org.clojure"]
               (map #(first-in % [::pom/dependency ::pom/groupId])
                    (deep-content xml [::pom/project ::pom/dependencies]))))
        (is (= ["clojure" "rome" "ring" "ring-codec" "ring-headers"
                "commons-codec" "commons-math" "commons-csv"
                "tools.emitter.jvm" "tools.namespace"]
               (map #(first-in % [::pom/dependency ::pom/artifactId])
                    (deep-content xml [::pom/project ::pom/dependencies]))))
        (is (= [nil nil nil nil nil "1.6" nil nil "0.1.0-beta5" "0.3.0-alpha3"]
               (map #(first-in % [::pom/dependency ::pom/version])
                    (deep-content xml [::pom/project ::pom/dependencies])))))
      (testing "managed dependencies are written to pom properly"
        (is (= ["org.clojure" "rome" "ring" "ring" "ring" "commons-math"
                "org.apache.commons" "ring" "org.clojure"]
               (map #(first-in % [::pom/dependency ::pom/groupId])
                    (deep-content xml [::pom/project ::pom/dependencyManagement ::pom/dependencies]))))
        (is (= ["clojure" "rome" "ring" "ring-codec" "ring-headers"
                "commons-math" "commons-csv" "ring-defaults" "tools.reader"]
               (map #(first-in % [::pom/dependency ::pom/artifactId])
                    (deep-content xml [::pom/project ::pom/dependencyManagement ::pom/dependencies]))))
        (is (= ["1.3.0" "0.9" "1.0.0" "1.0.1" "0.2.0" "1.2" "1.4" "0.2.1" "1.0.0-beta3"]
               (map #(first-in % [::pom/dependency ::pom/version])
                    (deep-content xml [::pom/project ::pom/dependencyManagement ::pom/dependencies]))))
        (is (= [nil nil nil nil nil "sources" "sources" nil nil]
               (map #(first-in % [::pom/dependency ::pom/classifier])
                    (deep-content xml [::pom/project ::pom/dependencyManagement ::pom/dependencies]))))))))

(deftest test-pom-plugins
  (let [xml              (parse-xml (make-pom with-pom-plugins-project))
        plugins          (deep-content xml [::pom/project ::pom/build ::pom/plugins])
        get-plugin       (fn [re]
                           (first (filter #(re-find re (pr-str %)) plugins)))
        simple-plugin    (get-plugin #"simple-plugin")
        plugin-with-vec  (get-plugin #"with-vec")
        plugin-with-map  (get-plugin #"with-map")
        plugin-with-list (get-plugin #"with-list")]
    (testing "two-parameter version adds maven plugin"
      (is (= simple-plugin
             (xml/sexp-as-element
               [::pom/plugin
                [::pom/groupId "two.parameter"]
                [::pom/artifactId "simple-plugin"]
                [::pom/version "1.0.0"]]))))
    (testing "vector as third parameter is interpreted as a mapping"
      (is (= plugin-with-vec
             (xml/sexp-as-element
               [::pom/plugin
                [::pom/groupId "three.parameter"]
                [::pom/artifactId "with-vec"]
                [::pom/version "1.0.1"]
                [::pom/a 3]]))))
    (testing "hashmap as third parameter is converted to tags"
      (is (= plugin-with-map
             (xml/sexp-as-element
               [::pom/plugin
                [::pom/groupId "three.parameter"]
                [::pom/artifactId "with-map"]
                [::pom/version "1.0.2"]
                [::pom/a 1]
                [::pom/b 2]
                [::pom/c 3]]))))
    (testing "list as third parameter keeps structure"
      (is (= plugin-with-list
             (xml/sexp-as-element
               [::pom/plugin
                [::pom/groupId "three.parameter"]
                [::pom/artifactId "with-list"]
                [::pom/version "1.0.3"]
                [::pom/root
                 [::pom/a 1]
                 [::pom/b
                  [::pom/c 2]
                  [::pom/d 3]]]]))))))
