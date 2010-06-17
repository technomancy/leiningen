(ns leiningen.new
  "Create a new project skeleton.
lein new [group-id/]artifact-id [project-dir]
Group-id is optional. Project-dir defaults to artifact-id if not given.
Neither group-id nor artifact-id may contain slashes."
  (:use [leiningen.core :only [ns->path]]
        [clojure.java.io :only [file]]
        [clojure.contrib.string :only [join]]))

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
       (.mkdirs (file project-dir))
       (spit (file project-dir "project.clj")
             (str "(defproject " project-name " \"1.0.0-SNAPSHOT\"\n"
                  "  :description \"FIXME: write\"\n"
                  "  :dependencies [[org.clojure/clojure \"1.1.0\"]\n"
                  "                 [org.clojure/clojure-contrib \"1.1.0\"]])"))
       (let [project-ns  (str (.replace (str project-name) "/" ".") ".core")
             project-clj (ns->path project-ns)
             test-clj (.replace project-clj ".clj" "_test.clj")]
         (.mkdirs (file project-dir "test"))
         (.mkdirs (.getParentFile (file project-dir "src" project-clj)))
         (spit (file project-dir "src" project-clj)
               (str "(ns " project-ns ")\n"))
         (.mkdirs (.getParentFile (file project-dir "test" test-clj)))
         (spit (file project-dir "test" test-clj)
               (str "(ns " (str project-ns "-test")
                    "\n  (:use [" project-ns "] :reload-all)"
                    "\n  (:use [clojure.test]))\n\n"
                    "(deftest replace-me ;; FIXME: write\n  (is false "
                    "\"No tests have been written.\"))\n"))
         (spit (file project-dir ".gitignore")
               (join "\n" ["pom.xml" "*jar" "lib" "classes"]))
         (spit (file project-dir "README")
               (join "\n\n" [(str "# " artifact-id)
                                 "FIXME: write description"
                                 "## Usage" "FIXME: write"
                                 "## Installation" "FIXME: write"
                                 "## License" "Copyright (C) 2010 FIXME"
                                 (str "Distributed under the Eclipse Public"
                                      " License, the same as Clojure.\n")]))
         (println "Created new project in:" project-dir))))
  ([project-name] (leiningen.new/new project-name
                                     (name (symbol project-name)))))
