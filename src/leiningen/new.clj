(ns leiningen.new
  "Create a new project skeleton."
  (:use [leiningen.core :only [abort user-settings]]
        [leiningen.util.paths :only [ns->path]]
        [clojure.java.io :only [file]]
        [clojure.string :only [join]])
  (:import (java.util Calendar)))

(defn format-settings [settings]
  (letfn [(format-map [m]
            (map #(str "  " %1 " " %2)
                 (map str (keys m))
                 (map str (vals m))))]
    (apply str
           (interpose "\n"
                      (format-map settings)))))

(defn write-project [project-dir project-name]
  (let [default-settings {:dependencies [['org.clojure/clojure "1.2.1"]]}
        settings  (merge-with #(if %2 %2 %1)
                              default-settings
                              (user-settings))]
    (.mkdirs (file project-dir))
    (spit (file project-dir "project.clj")
          (str "(defproject " project-name " \"1.0.0-SNAPSHOT\"\n"
               "  :description \"FIXME: write description\"\n"
               (format-settings (into (sorted-map) settings))
               ")" ))))

(defn write-implementation [project-dir project-clj project-ns]
  (.mkdirs (.getParentFile (file project-dir "src" project-clj)))
  (spit (file project-dir "src" project-clj)
        (str "(ns " project-ns ")\n")))

(defn write-test [project-dir test-ns project-ns]
  (.mkdirs (.getParentFile (file project-dir "test" (ns->path test-ns))))
  (spit (file project-dir "test" (ns->path test-ns))
        (str "(ns " (str test-ns)
             "\n  (:use [" project-ns "])"
             "\n  (:use [clojure.test]))\n\n"
             "(deftest replace-me ;; FIXME: write\n  (is false "
             "\"No tests have been written.\"))\n")))

(defn- year []
  (.get (Calendar/getInstance) Calendar/YEAR))

(defn write-readme [project-dir artifact-id]
  (spit (file project-dir "README")
        (join "\n\n" [(str "# " artifact-id)
                      "FIXME: write description"
                      "## Usage" "FIXME: write"
                      "## License" (str "Copyright (C) " (year) " FIXME")
                      (str "Distributed under the Eclipse Public"
                           " License, the same as Clojure.\n")])))

(def project-name-blacklist #"(?i)(?<!(clo|compo))jure")

(defn new
  "Create a new project skeleton."
  ([project-name]
     (leiningen.new/new project-name (name (symbol project-name))))
  ([project-name project-dir]
     (when (re-find project-name-blacklist project-name)
       (abort "Sorry, *jure names are no longer allowed."))
     (try (read-string project-name)
          (catch Exception _
            (abort "Sorry, project names must be valid Clojure symbols.")))
     (let [project-name (symbol project-name)
           group-id (namespace project-name)
           artifact-id (name project-name)
           project-dir (-> (System/getProperty "leiningen.original.pwd")
                           (file project-dir)
                           (.getAbsolutePath ))]
       (write-project project-dir project-name)
       (let [prefix (.replace (str project-name) "/" ".")
             project-ns (str prefix ".core")
             test-ns (str prefix ".test.core")
             project-clj (ns->path project-ns)]
         (spit (file project-dir ".gitignore")
               (apply str (interleave ["pom.xml" "*jar" "/lib/" "/classes/"
                                       ".lein-failures" ".lein-deps-sum"]
                                      (repeat "\n"))))
         (write-implementation project-dir project-clj project-ns)
         (write-test project-dir test-ns project-ns)
         (write-readme project-dir artifact-id)
         (println "Created new project in:" project-dir)))))
