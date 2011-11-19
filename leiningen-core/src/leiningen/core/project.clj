(ns leiningen.core.project
  (:require [clojure.walk :as walk]
            [clojure.java.io :as io]))

(defn- unquote-project [args]
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
               :jar-exclusions [#"^\."]
               :uberjar-exclusions [#"^META-INF/DUMMY.SF"]})

(defmacro defproject [project-name version & {:as args}]
  `(let [args# (apply hash-map ~(cons 'list (unquote-project ~args)))]
     (def ~'project
       (merge defaults args#
              {:name ~(name project-name)
               :group ~(or (namespace project-name)
                           (name project-name))
               :version ~version
               :dependencies (or (:dependencies args#) (:deps args#))
               :dev-dependencies (or (:dev-dependencies args#) (:dev-deps args#))
               :root ~(.getParent (io/file *file*))}))))

(defn read-project
  ([file]
     (try (binding [*ns* (the-ns 'leiningen.core)]
            (load-file file))
          (if-let [project (resolve 'user/project)]
            @project
            (throw (Exception. "project.clj must define user/project map.")))
          (catch java.io.FileNotFoundException _)))
  ([] (read-project "project.clj")))
