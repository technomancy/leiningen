(ns leiningen.new
  "Create a new project skeleton.
lein new [group-id/]artifact-id [project-dir]
Group-id is optional. Project-dir defaults to artifact-id if not given.
Neither group-id nor artifact-id may contain slashes."
  (:use [leiningen.core :only [ns->path]]
        [clojure.java.io :only [file]]
        [clojure.string :only [join]]))

(defn write-project [project-dir project-name]
  (.mkdirs (file project-dir))
  (spit (file project-dir "project.clj")
        (str "(defproject " project-name " \"1.0.0-SNAPSHOT\"\n"
             "  :description \"FIXME: write\"\n"
             "  :dependencies [[org.clojure/clojure \"1.2.0\"]\n  "
             "                 [org.clojure/clojure-contrib \"1.2.0\"]])\n")))

(defn write-implementation [project-dir project-clj project-ns]
  (.mkdirs (.getParentFile (file project-dir "src" project-clj)))
  (spit (file project-dir "src" project-clj)
        (str "(ns " project-ns ")\n")))

(defn write-test [project-dir test-ns project-ns]
  (.mkdirs (.getParentFile (file project-dir "test" (ns->path test-ns))))
  (spit (file project-dir "test" (ns->path test-ns))
        (str "(ns " (str test-ns)
             "\n  (:use [" project-ns "] :reload)"
             "\n  (:use [clojure.test]))\n\n"
             "(deftest replace-me ;; FIXME: write\n  (is false "
             "\"No tests have been written.\"))\n")))

(defn write-readme [project-dir artifact-id]
  (spit (file project-dir "README")
        (join "\n\n" [(str "# " artifact-id)
                      "FIXME: write description"
                      "## Usage" "FIXME: write"
                      "## Installation" "FIXME: write"
                      "## License" "Copyright (C) 2010 FIXME"
                      (str "Distributed under the Eclipse Public"
                           " License, the same as Clojure.\n")])))

(defn new
  "Create a new project skeleton.
lein new [group-id/]artifact-id [project-dir]
Group-id is optional. Project-dir defaults to artifact-id if not given.
Neither group-id nor artifact-id may contain slashes."
  ([project-name project-dir]
     (when (re-find #"(?<!clo)jure" project-name)
       (throw (IllegalArgumentException. "*jure names are no longer allowed.")))
     (let [project-name (symbol project-name)
           group-id (namespace project-name)
           artifact-id (name project-name)]
       (write-project project-dir project-name)
       (let [prefix (.replace (str project-name) "/" ".")
             project-ns (str prefix ".core")
             test-ns (str prefix ".test.core")
             project-clj (ns->path project-ns)]
         (spit (file project-dir ".gitignore")
               (apply str (interleave ["pom.xml" "*jar" "lib" "classes"]
                                      (repeat "\n"))))
         (write-implementation project-dir project-clj project-ns)
         (write-test project-dir test-ns project-ns)
         (write-readme project-dir artifact-id)
         (println "Created new project in:" project-dir))))
  ([project-name] (leiningen.new/new project-name
                                     (name (symbol project-name)))))
