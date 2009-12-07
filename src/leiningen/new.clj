(ns leiningen.new
  "Create a new project skeleton.
lein new [group-id/]artifact-id [project-dir]
Group-id is optional. Project-dir defaults to artifact-id if not given.
Neither group-id nor artifact-id may contain slashes."
  (:use [clojure.contrib.duck-streams :only [spit]]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.str-utils :only [str-join]]))

(defn str-replace [subs s]
  (apply str (replace subs s)))

(defn new [project-name & [_ project-dir]]
  ; Clojure symbols conventionally use -, not _ to separate words in a
  ; compound name.  When such compound names are used for as
  ; directories or java packages, however, we must use _ in place of -.
  (let [project-name (symbol (str-replace {\_ \-} project-name))
        artifact-id (name project-name)
        project-dir (or project-dir (str-replace {\- \_} artifact-id))]
    (.mkdirs (file project-dir))
    (spit (file project-dir "project.clj")
          (pr-str (list 'defproject project-name "1.0.0-SNAPSHOT"
                        :description "FIXME: write"
                        :dependencies [['org.clojure/clojure
                                        "1.1.0-alpha-SNAPSHOT"]
                                       ['org.clojure/clojure-contrib
                                        "1.0-SNAPSHOT"]])))
    (let [starter-clj-ns   (str-replace {\/ \.} (str project-name))
          starter-clj (str (str-replace {\- \_ \. \/} starter-clj-ns) ".clj")
          starter-clj-dir  (.getParent (file starter-clj))]
      (doseq [d [(str "src/" starter-clj-dir) "test" "lib" "classes"]]
        (.mkdirs (file project-dir d)))
      ;; maybe keep this somewhere else?
      (spit (file project-dir "src" starter-clj)
            (str "(ns " starter-clj-ns ")\n"))
      (spit (file project-dir ".gitignore")
            (str-join "\n" ["pom-generated.xml"
                            "Manifest.txt"
                            (str artifact-id ".jar")]))
      (spit (file project-dir "lib" ".gitignore") "*")
      (spit (file project-dir "classes" ".gitignore") "*")
      (spit (file project-dir "README")
            (str-join "\n\n" [(str "# " project-name)
                              "FIXME: write description"
                              "## Usage" "FIXME: write"
                              "## Installation" "FIXME: write"
                              "## License" "FIXME: write\n"]))
      (println "Created new project in:" project-dir))))
