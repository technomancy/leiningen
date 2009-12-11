(ns leiningen.new
  "Create a new project skeleton.
lein new [group-id/]artifact-id [project-dir]
Group-id is optional. Project-dir defaults to artifact-id if not given.
Neither group-id nor artifact-id may contain slashes."
  (:use [clojure.contrib.duck-streams :only [spit]]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.str-utils :only [str-join]]))

(defn new
  ([project-name project-dir]
     (let [project-name (symbol project-name)
           group-id (namespace project-name)
           artifact-id (name project-name)]
       (.mkdirs (file project-dir))
       ;; TODO: pretty-print this
       (spit (file project-dir "project.clj")
             (pr-str (list 'defproject project-name "1.0.0-SNAPSHOT"
                           :description "FIXME: write"
                           :dependencies [['org.clojure/clojure
                                           "1.1.0-alpha-SNAPSHOT"]
                                          ['org.clojure/clojure-contrib
                                           "1.0-SNAPSHOT"]])))
       (let [project-ns  (.replace (str project-name) "/" ".")
             project-clj (str (.replace (str project-name) "-" "_") ".clj")]
         (.mkdirs (file project-dir "test"))
         (.mkdirs (.getParentFile (file project-dir "src" project-clj)))
         (spit (file project-dir "src" project-clj)
               (str "(ns " project-ns ")\n"))
         (spit (file project-dir ".gitignore")
               (str-join "\n" ["pom-generated.xml"
                               "Manifest.txt"
                               (str artifact-id ".jar")
                               "lib" "classes"]))
         (spit (file project-dir "README")
               (str-join "\n\n" [(str "# " artifact-id)
                                 "FIXME: write description"
                                 "## Usage" "FIXME: write"
                                 "## Installation" "FIXME: write"
                                 "## License" "FIXME: write\n"]))
         (println "Created new project in:" project-dir))))
  ([project-name] (leiningen.new/new project-name
                                     (name (symbol project-name)))))
