(ns leiningen.core.project
  "Read project.clj files."
  (:refer-clojure :exclude [read])
  (:require [clojure.walk :as walk]
            [clojure.java.io :as io]))

(defn- unquote-project
  "Inside defproject forms, unquoting (~) allows for arbitrary evaluation."
  [args]
  (walk/walk (fn [item]
               (cond (and (seq? item) (= `unquote (first item))) (second item)
                     ;; needed if we want fn literals preserved
                     (or (seq? item) (symbol? item)) (list 'quote item)
                     :else (unquote-project item)))
             identity
             args))

(def defaults {:source-path "src"
               :compile-path "classes"
               :resources-path "resources"
               :test-path "test"
               :dev-resources-path "dev-resources"
               :native-path "native"
               :target-path "target"
               :repositories [["central" "http://repo1.maven.org/maven2"]
                              ;; TODO: point to releases-only before 2.0 is out
                              ["clojars" "http://clojars.org/repo/"]]
               :jar-exclusions [#"^\."]
               :uberjar-exclusions [#"^META-INF/DUMMY.SF"]})

(defn ^:internal add-repositories
  "Public only for macroexpansion purposes, :repositories needs special
  casing logic for merging default values with user-provided ones."
  [{:keys [omit-default-repositories repositories] :as
  project}]
  (assoc project :repositories
         (concat repositories (if-not omit-default-repositories
                                (:repositories defaults)))))

(defmacro defproject
  "The project.clj file must either def a project map or call this macro."
  [project-name version & {:as args}]
  `(let [args# (apply hash-map [~@(unquote-project args)])]
     (def ~'project
       (merge defaults (add-repositories args#)
              {:name ~(name project-name)
               :group ~(or (namespace project-name)
                           (name project-name))
               :version ~version
               :dependencies (or (:dependencies args#) (:deps args#))
               :dev-dependencies (or (:dev-dependencies args#) (:dev-deps args#))
               :root ~(.getParent (io/file *file*))}))))

(defn read
  "Read project map out of file, which defaults to project.clj."
  ([file]
     (try (binding [*ns* (find-ns 'leiningen.core.project)]
            (load-file file))
          (let [project (resolve 'leiningen.core.project/project)]
            (when-not project
              (throw (Exception. "project.clj must define project map.")))
            (ns-unmap *ns* 'project) ; return it to original state
            @project)
          (catch java.io.FileNotFoundException _)))
  ([] (read "project.clj")))
