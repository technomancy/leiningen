(ns leiningen.new
  "Create a new project skeleton."
  (:use [clojure.contrib.duck-streams :only [spit]]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.str-utils :only [str-join]]))

(defn new [project-name & [args]]
  (.mkdirs (file project-name))
  (spit (file project-name "project.clj")
        (pr-str (list 'defproject (symbol "leiningen") "1.0.0-SNAPSHOT"
                      :description "FIXME: write"
                      :dependencies [['org.clojure/clojure
                                      "1.1.0-alpha-SNAPSHOT"]
                                     ['org.clojure/clojure-contrib
                                      "1.0-SNAPSHOT"]])))
  (doseq [d [(str "src/" project-name) "test" "lib" "classes"]]
    (.mkdirs (file project-name d)))
  ;; maybe keep this somewhere else?
  (spit (file project-name "src" (str project-name ".clj"))
        (str "(ns " project-name ".core)\n"))
  (spit (file project-name ".gitignore")
        (str-join "\n" ["pom-generated.xml"
                        "Manifest.txt"
                        (str project-name ".jar")]))
  (spit (file project-name "lib" ".gitignore") "*")
  (spit (file project-name "classes" ".gitignore") "*")
  (spit (file project-name "README")
        (str-join "\n\n" [(str "# " project-name)
                          "FIXME: write description"
                          "## Usage" "FIXME: write"
                          "## Installation" "FIXME: write"
                          "## License" "FIXME: write\n"]))
  (println "Created new project:" project-name))
