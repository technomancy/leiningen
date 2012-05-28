(ns leiningen.test.pom
  (:use [clojure.test]
        [clojure.java.io :only [file delete-file]]
        [leiningen.pom :only [make-pom pom]]
        [leiningen.core.user :as user]
        [leiningen.test.helper :only [sample-project]])
  (:require [clojure.data.xml :as xml]
            [leiningen.core.project :as project]))

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

(defn with-profile
  ([project profile]
     (with-profile project :test-pom profile))
  ([project name profile]
     (let [{:keys [included-profiles
                   without-profiles]} (meta project)]
       (-> without-profiles
           (update-in [:profiles] #(assoc % name profile))
           (project/merge-profiles
            (if (some #{name} included-profiles)
              included-profiles
              (conj included-profiles name)))))))

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
    (is (= nil (first-in xml [:project :parent]))
        "no parent")
    (is (= nil (first-in xml [:project :url]))
        "no url")
    (is (= nil (first-in xml [:project :licenses]))
        "no license")
    (is (= "A test project" (first-in xml [:project :description]))
        "description is included")
    (is (= nil (first-in xml [:project :mailingLists]))
        "no mailing list")
    (is (= ["central" "clojars" "snapshots"]
           (map #(first-in % [:repository :id])
                (deep-content xml [:project :repositories])))
        "repositories are named")
    (is (= ["http://repo1.maven.org/maven2" "https://clojars.org/repo/"
            (format "file://%s/lein-repo"
                    (System/getProperty "java.io.tmpdir"))]
           (map #(first-in % [:repository :url])
                (deep-content xml [:project :repositories])))
        "repositories have correct location")
    (is (= ["true" "true" "true"]
           (map #(first-in % [:repository :snapshots :enabled])
                (deep-content xml [:project :repositories])))
        "snapshots are enabled")
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
    (is (= ["dev-resources" "resources"]
           (map #(first-in % [:testResource :directory])
                (deep-content xml [:project :build :testResources])))
        "test resource directories use :dev :default and :test profiles")
    (is (= "target" (first-in xml [:project :build :directory]))
        "target directory is included")
    (is (= nil (first-in xml [:project :build :extensions]))
        "no extensions")
    (is (= "target/classes" (first-in xml [:project :build :outputDirectory]))
        "classes directory is included")
    (is (= ["org.clojure" "rome" "ring"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.1.0" "0.9" "1.0.0"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))))

(deftest test-dependencies-are-test-scoped
  (let [xml (xml/parse-str
             (make-pom (with-profile
                         sample-project
                         :test
                         {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.1.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "test"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))))

(deftest dev-dependencies-are-test-scoped
  (let [xml (xml/parse-str
             (make-pom (with-profile
                         sample-project
                         :dev
                         {:dependencies '[[peridot "0.0.5"]]})))]
    (is (= ["org.clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "peridot"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.1.0" "0.9" "1.0.0" "0.0.5"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "test"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))))

(deftest dependency-options
  (let [xml (xml/parse-str
             (make-pom (with-profile
                         sample-project
                         {:dependencies '[[peridot "0.0.5"
                                           :scope "provided"
                                           :optional true
                                           :classifier "sources"
                                           :extension "pom"
                                           :exclusions
                                           [[ring-mock
                                             :classifier "cla"
                                             :extension "dom"]]]]})))]
    (is (= ["peridot" "org.clojure" "rome" "ring"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= [ "peridot" "clojure" "rome" "ring"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["0.0.5" "1.1.0" "0.9" "1.0.0"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= ["provided" nil nil nil]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))
    (is (= ["true" nil nil nil]
           (map #(first-in % [:dependency :optional])
                (deep-content xml [:project :dependencies]))))
    (is (= ["sources" nil nil nil]
           (map #(first-in % [:dependency :classifier])
                (deep-content xml [:project :dependencies]))))
    (is (= ["pom" nil nil nil]
           (map #(first-in % [:dependency :type])
                (deep-content xml [:project :dependencies]))))
    (is (= ["ring-mock" nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["ring-mock" nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["cla" nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :classifier])
                (deep-content xml [:project :dependencies]))))
    (is (= ["dom" nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :type])
                (deep-content xml [:project :dependencies])))))
  (let [xml (xml/parse-str
             (make-pom (with-profile
                         sample-project
                         {:dependencies '[[peridot "0.0.5"
                                           :scope "provided"
                                           :exclusions
                                           [ring-mock]]]})))]
    (is (= ["peridot" "org.clojure" "rome" "ring"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= [ "peridot" "clojure" "rome" "ring"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["0.0.5" "1.1.0" "0.9" "1.0.0"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= ["provided" nil nil nil]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))
    (is (= ["ring-mock" nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :classifier])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil nil]
           (map #(first-in % [:dependency :exclusions :exclusion :type])
                (deep-content xml [:project :dependencies]))))))

(deftest dependencies-are-required-when-overlapped-by-builtin-profiles
  (let [xml (xml/parse-str
             (make-pom (with-profile
                         sample-project
                         :dev {:dependencies '[[rome "0.8"]]})))]
    (is (= ["org.clojure" "rome" "ring" "rome"]
           (map #(first-in % [:dependency :groupId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["clojure" "rome" "ring" "rome"]
           (map #(first-in % [:dependency :artifactId])
                (deep-content xml [:project :dependencies]))))
    (is (= ["1.1.0" "0.9" "1.0.0" "0.8"]
           (map #(first-in % [:dependency :version])
                (deep-content xml [:project :dependencies]))))
    (is (= [nil nil nil "test"]
           (map #(first-in % [:dependency :scope])
                (deep-content xml [:project :dependencies]))))))

(deftest test-pom-has-classifier-when-defined
  (is (not (re-find #"classifier"
                    (make-pom sample-project))))
  (is (= "stuff"
         (-> (make-pom (with-profile
                         sample-project
                          {:classifier "stuff"}))
              xml/parse-str
              (first-in [:project :classifier])))))

(deftest test-pom-adds-java-source-paths
  (is (= ["java/src" "java/another"]
         (-> (make-pom (with-profile sample-project
                         {:java-source-paths ["java/src" "java/another"]}))
             xml/parse-str
             (deep-content [:project :build :plugins :plugin :executions
                            :execution :configuration :sources])
             ((partial mapcat :content))))))

(deftest test-pom-handles-global-exclusions
  (is (= [["clojure"] ["clojure"] ["clojure"]]
         (-> (make-pom (with-profile sample-project
                         {:exclusions '[org.clojure/clojure]}))
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
  (binding [leiningen.core.main/*exit-process?* false]
    (let [project (assoc sample-project :version "1.0"
                         :dependencies [['clojure "1.0.0-SNAPSHOT"]])]
      (is (thrown? Exception (pom project))))))
