(ns leiningen.core.test.project
  (:refer-clojure :exclude [read])
  (:use [clojure.test]
        [leiningen.core.project]))

(deftest test-read-project
  (is (= {:name "leiningen", :group "leiningen", :version "2.0.0-SNAPSHOT",
          :url "https://github.com/technomancy/leiningen"

          :source-path ["src"],
          :compile-path "classes",
          :test-path [],
          :resources-path ["resources"],
          :native-path ["native"],
          :target-path "target",

          :disable-implicit-clean true,
          :eval-in :leiningen,
          :license {:name "Eclipse Public License"}

          :dependencies '[[leiningen-core "2.0.0-SNAPSHOT"]
                          [clucy "0.2.2"] [lancet "1.0.1"]
                          [robert/hooke "1.1.2"]
                          [stencil "0.2.0"]],
          :twelve 12 ; testing unquote

          ;; wtf, (= [#"^\."] [#"^\."]) <- false
          ;; :jar-exclusions [#"^\."],
          ;; :uberjar-exclusions [#"^META-INF/DUMMY.SF"],
          :repositories [["central" {:url "http://repo1.maven.org/maven2"}]
                         ["clojars" {:url "http://clojars.org/repo/"}]]}
         (dissoc (read "dev-resources/p1.clj")
                 :description :root :jar-exclusions :uberjar-exclusions))))

;; TODO: test omit-default
;; TODO: test reading project that doesn't def project