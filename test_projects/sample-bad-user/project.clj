(def clj-version "1.3.0")

(defproject nomnomnom "0.5.0-SNAPSHOT"
  :description "A test project"
  :url "http://leiningen.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[~(symbol "org.clojure" "clojure") ~clj-version]
                 [rome ~(str "0." "9")]
                 [ring "1.0.0"]]
  :plugins [[codox "0.6.4"]]
  :main nom.nom.nom
  :global-vars {*warn-on-reflection* true}
  :jar-exclusions [#"^META-INF"]
  :filespecs [{:type :fn :fn (fn [p] {:type :bytes :path "bytes.clj"
                                     :bytes (str "[:bytes \"are\" "
                                                 (:name p) "]")})}]
  :test-selectors {:integration :integration
                   :default (complement :integration)
                   :random (fn [_] (> (rand) ~(float 1/2)))}
  :repositories [["other" {:url "http://example.com/repo"
                           :update :always
                           :releases {:checksum :warn}}]]
  :deploy-repositories {"snapshots" ~(format "file://%s/lein-repo"
                                             (System/getProperty "java.io.tmpdir"))})
