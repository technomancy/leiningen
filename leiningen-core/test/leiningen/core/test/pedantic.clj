(ns leiningen.core.test.pedantic
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [leiningen.core.classpath :as cp]
            [leiningen.core.pedantic :as pedantic]
            [cemerick.pomegranate.aether :as aether])
  (:import (org.eclipse.aether.graph DependencyNode)))

(def tmp-dir (io/file
              (System/getProperty "java.io.tmpdir") "pedantic"))
(def tmp-local-repo-dir (io/file tmp-dir "local-repo"))

(defn delete-recursive
  [dir]
  (when (.isDirectory dir)
    (doseq [file (.listFiles dir)]
      (delete-recursive file)))
    (.delete dir))

(defn clear-tmp
  [f]
  (delete-recursive (io/file tmp-dir)) (f))

(defn get-versions [name repo]
  (let [name (symbol name)]
    (map second (filter #(= name (first %)) (keys repo)))))

(defn make-pom-string [name version deps]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
  <project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">
  <modelVersion>4.0.0</modelVersion>
  <groupId>" name "</groupId>
  <artifactId>" name "</artifactId>
  <packaging>jar</packaging>
  <version>" version "</version>
  <name>" name "</name>"
  (if-not (empty? deps)
    (apply str
           "<dependencies>"
           (clojure.string/join "\n"
                                (for [[n v] deps]
                                  (str "<dependency>
                   <groupId>" n "</groupId>
                   <artifactId>"n"</artifactId>
                   <version>"v"</version>
                   </dependency>")))
           "</dependencies>"))
  " </project>"))

(defn make-metadata [name versions]
  (str "<metadata>
  <groupId>" name "</groupId>
  <artifactId>" name "</artifactId>
  <versioning>
  <versions>"
  (clojure.string/join "\n"
                       (for [v versions]
                         (str "<version>"v"</version>")))
    "</versions>
    <lastUpdated>20120810193549</lastUpdated>
  </versioning>
  </metadata>"))

(defn add-repo [repo]
  (fn [f]
    (aether/register-wagon-factory!
     "fake"
     #(reify org.apache.maven.wagon.Wagon
        (getRepository [_]
          (proxy [org.apache.maven.wagon.repository.Repository] []))
        (^void connect [_
                        ^org.apache.maven.wagon.repository.Repository _
                        ^org.apache.maven.wagon.authentication.AuthenticationInfo _
                        ^org.apache.maven.wagon.proxy.ProxyInfoProvider _])
        (disconnect [_])
        (removeTransferListener [_ _])
        (addTransferListener [_ _])
        (setTimeout [_ _])
        (setInteractive [_ _])
        (get [_ name file]
          (let [[n _ version] (clojure.string/split name #"/")]
            (if (= name (str n "/" n "/maven-metadata.xml"))
              (if-let [versions (get-versions n repo)]
                (spit file (make-metadata n versions))
                (spit file ""))
              (if-let [deps (repo [(symbol n) version])]
                (if (re-find #".pom$" name)
                  (spit file (make-pom-string n version deps))
                  (spit file ""))
                (throw (org.apache.maven.wagon.ResourceDoesNotExistException. ""))))))))
    (f)))

(defn resolve-deps [ranges overrides coords]
  (aether/resolve-dependencies
   :coordinates coords
   :repositories {"test-repo" {:url "fake://ss"
                               :checksum :warn}}
   :local-repo tmp-local-repo-dir
   :repository-session-fn
   #(-> %
        aether/repository-session
        (#'pedantic/use-transformer ranges overrides))))

(defmulti translate type)

(defmethod translate :default [x] x)

(defmethod translate java.util.List
  [l]
  (vec (remove nil? (map translate l))))

(defmethod translate java.util.Map
  [m]
  (into {} (map (fn [[k v]] [k (translate v)]) m)))

(defn- node->artifact-map
  [^DependencyNode node]
  (if-let [d (.getDependency node)]
    (if-let [a (.getArtifact d)]
      (let [b (bean a)]
        (-> b
            (select-keys [:artifactId :groupId :exclusions
                          :version :extension :properties])
            (update-in [:exclusions] vec))))))

(defmethod translate DependencyNode
  [n]
  (if-let [a (node->artifact-map n)]
    [(if (= (:groupId a) (:artifactId a))
       (symbol (:artifactId a))
       (symbol (:groupId a) (:artifactId a)))
     (:version a)]))

(def repo
  '{[a "1"] []
    [a "2"] []
    [aa "2"] [[a "2"]]
    [range "1"] [[a "[1,)"]]
    [range "2"] [[a "[2,)"]]})

(use-fixtures :once (add-repo repo))

(deftest top-level-overrides-transative-later
  (let [ranges (atom [])
        overrides (atom [])]
    (resolve-deps ranges overrides
                  '[[a "1"]
                    [aa "2"]])
    (is (= @ranges []))
    (is (= (translate @overrides)
           '[{:accepted {:node [a "1"]
                         :parents []}
              :ignoreds [{:node [a "2"]
                          :parents [[aa "2"]]}]
              :ranges []}]))))

(deftest ranges-are-found
  (let [ranges (atom [])
        overrides (atom [])]
    (resolve-deps ranges overrides '[[range "1"]])
    (is (= (translate @ranges) '[{:node [a "1"]
                                  :parents [[range "1"]]}
                                 {:node [a "2"]
                                  :parents [[range "1"]]}]))
    (is (= @overrides []))))

(deftest range-causes-other-transative-to-ignore-top-level
  (let [ranges (atom [])
        overrides (atom [])]
    (resolve-deps ranges overrides '[[a "1"]
                                     [aa "2"]
                                     [range "2"]])
    (is (= (translate @ranges) '[{:node [a "2"]
                                  :parents [[range "2"]]}]))
    (is (= (translate @overrides)
           '[{:accepted {:node [a "2"]
                         :parents [[aa "2"]]}
              :ignoreds [{:node [a "1"]
                          :parents []}]
              :ranges []}]))))

(deftest netty-boringssl-works
  (let [project {:root "/tmp"
                 :dependencies '[[io.netty/netty-tcnative-boringssl-static
                                  "2.0.50.Final"]]
                 :pedantic? :warn
                 :repositories [["c" {:url "https://repo1.maven.org/maven2/"
                                      :snapshots false}]]}]
    ;; this will result in an infinite loop in lein 2.9.8
    (is (cp/get-classpath project))))

(deftest ^:online multiple-paths-to-ignored-dep
  (let [ranges (atom [])
        overrides (atom [])]
    (aether/resolve-dependencies
     :coordinates '[[com.amazonaws/aws-java-sdk-s3 "1.12.402"]]
     :repository-session-fn
     #(-> %
          aether/repository-session
          (#'pedantic/use-transformer ranges overrides)))

    (is (empty? @ranges))
    (is (= (translate @overrides)
           '[{:accepted {:node    [commons-logging "1.1.3"]
                         :parents [[com.amazonaws/aws-java-sdk-s3 "1.12.402"]
                                   [com.amazonaws/aws-java-sdk-core "1.12.402"]]}
              :ignoreds [; leiningen <= 2.10 used to also report this path,
                                        ; now we only report the shortest path
                         #_{:node    [commons-logging "1.2"]
                            :parents [[com.amazonaws/aws-java-sdk-s3 "1.12.402"]
                                      [com.amazonaws/aws-java-sdk-kms "1.12.402"]
                                      [com.amazonaws/aws-java-sdk-core "1.12.402"]
                                      [org.apache.httpcomponents/httpclient "4.5.13"]]}
                         {:node    [commons-logging "1.2"]
                          :parents [[com.amazonaws/aws-java-sdk-s3 "1.12.402"]
                                    [com.amazonaws/aws-java-sdk-core "1.12.402"]
                                    [org.apache.httpcomponents/httpclient "4.5.13"]]}]
              :ranges   []}]))))

(deftest dont-suggest-on-duplicates
  (let [project {:root "/tmp"
                 :dependencies '([cider/cider-nrepl "0.44.0"]
                                 [cider/cider-nrepl "0.44.0"])
                 :pedantic? :abort}]
    (is (cp/get-dependencies :dependencies nil project))))
