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

(def defaults {:source-path ["src"]
               :resources-path ["resources"]
               :test-path []
               :native-path ["native"]
               :compile-path "target/classes"
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
         (for [[id repo] (concat repositories (if-not omit-default-repositories
                                                (:repositories defaults)))]
           [id (if (string? repo) {:url repo} repo)])))

(defmacro defproject
  "The project.clj file must either def a project map or call this macro."
  [project-name version & {:as args}]
  `(let [args# ~(unquote-project args)]
     (def ~'project
       (merge defaults (dissoc (add-repositories args#)
                               ;; Strip out aliases for normalization.
                               :eval-in-leiningen :deps)
              {:name ~(name project-name)
               :group ~(or (namespace project-name)
                           (name project-name))
               :version ~version
               :dependencies (or (:dependencies args#) (:deps args#))
               :compile-path (or (:compile-path args#)
                                 (.getPath (io/file (:target-path args#)
                                                    "classes")))
               :root ~(.getParent (io/file *file*))
               :eval-in (or (:eval-in args#)
                            (if (:eval-in-leiningen args#)
                              :leiningen
                              :subprocess))}))))

(defn read
  "Read project map out of file, which defaults to project.clj."
  ([file]
     (try (binding [*ns* (find-ns 'leiningen.core.project)]
            (load-file file))
          (let [project (resolve 'leiningen.core.project/project)]
            (when-not project
              (throw (Exception. "project.clj must define project map.")))
            ;; (ns-unmap *ns* 'project) ; return it to original state
            @project)
          (catch java.io.FileNotFoundException _)))
  ([] (read "project.clj")))
