;; This project is used for leiningen's test suite, so don't change
;; any of these values without updating the relevant tests. If you
;; just want a basic project to work from, generate a new one with
;; "lein new".

(def clj-version "1.3.0")

(defproject nomnomnom "0.5.0-SNAPSHOT"
  :description "A test project"
  :dependencies [[~(symbol "org.clojure" "clojure") ~clj-version]
                 [rome ~(str "0." "9")]
                 [ring "1.0.0"]]
  :main nom.nom.nom
  :warn-on-reflection true
  :shell-wrapper {:main nom.nom.nom
                  :bin "bin/nom"}
  :jar-exclusions [#"^META-INF"]
  :filespecs [{:type :fn :fn (fn [p] {:type :bytes :path "bytes.clj"
                                     :bytes (str "[:bytes \"are\" "
                                                 (:name p) "]")})}]
  :test-selectors {:integration :integration
                   :default (complement :integration)
                   :random (fn [_] (> (rand) ~(float 1/2)))
                   :all (constantly true)}
  :repositories {"snapshots" ~(format "file://%s/lein-repo"
                                      (System/getProperty "java.io.tmpdir"))})
