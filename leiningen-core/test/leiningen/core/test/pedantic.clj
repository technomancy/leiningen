(ns leiningen.core.test.pedantic
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [leiningen.core.pedantic :as pedantic]
            [cemerick.pomegranate.aether :as aether]))

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

(def ranges (atom []))
(def overrides (atom []))

(defn reset-state [f]
  (reset! ranges [])
  (reset! overrides [])
  (f))

(defn resolve-deps [coords]
  (aether/resolve-dependencies
   :coordinates coords
   :repositories {"test-repo" {:url "fake://ss"
                               :checksum false}}
   :local-repo tmp-local-repo-dir
   :repository-session-fn
   #(-> %
        aether/repository-session
        (#'pedantic/use-transformer ranges overrides))))

(defmulti translate type)

(defmethod translate :default [x] x)

(defmethod translate java.util.List
  [l]
  (remove nil? (map translate l)))

(defmethod translate java.util.Map
  [m]
  (into {} (map (fn [[k v]] [k (translate v)]) m)))

(defmethod translate org.eclipse.aether.graph.DependencyNode
  [n]
  (if-let [a (#'pedantic/node->artifact-map n)]
    [(symbol (:artifactId a)) (:version a)]))

(def repo
  '{[a "1"] []
    [a "2"] []
    [aa "2"] [[a "2"]]
    [range "1"] [[a "[1,)"]]
    [range "2"] [[a "[2,)"]]})

(use-fixtures :once (add-repo repo))
(use-fixtures :each clear-tmp)
(use-fixtures :each reset-state)


(deftest top-level-overrides-transative-later
  (resolve-deps '[[a "1"]
                  [aa "2"]])
  (is (= @ranges []))
  (is (= (translate @overrides)
         '[{:accepted {:node [a "1"]
                       :parents []}
            :ignoreds [{:node [a "2"]
                        :parents [[aa "2"]]}]
            :ranges []}])))

(deftest ranges-are-found
  (resolve-deps '[[range "1"]])
  (is (= (translate @ranges) '[{:node [a "1"]
                               :parents [[range "1"]]}
                               {:node [a "2"]
                               :parents [[range "1"]]}]))
  (is (= @overrides
         [])))

(deftest range-causes-other-transative-to-ignore-top-level
  (resolve-deps '[[a "1"]
                  [aa "2"]
                  [range "2"]])
  (is (= (translate @ranges) '[{:node [a "2"]
                                :parents [[range "2"]]}]))
  (is (= (translate @overrides)
         '[{:accepted {:node [a "2"]
                       :parents [[aa "2"]]}
            :ignoreds [{:node [a "1"]
                        :parents []}]
            :ranges [{:node [a "2"]
                      :parents [[range "2"]]}]}])))

