(ns leiningen.compile
  "Compile Clojure source into .class files."
  (:require [leiningen.core.user :as user]
            [leiningen.core.ns :as ns]
            [leiningen.core.eval :as eval]
            [clojure.java.io :as io])
  (:refer-clojure :exclude [compile])
  (:import (java.io PushbackReader)))

(defn- regex? [str-or-re]
  (instance? java.util.regex.Pattern str-or-re))

(defn- find-namespaces-by-regex [project nses]
  (let [[res syms] ((juxt filter remove) regex? nses)]
    (if (seq res)
      (set (for [re res n (mapcat ns/namespaces-in-dir (:source-path project))
                 :when (re-find re (name n))]
             n))
      nses)))

(defn- compile-main? [{:keys [main source-path] :as project}]
  (and main (not (:skip-aot (meta main)))
       (some #(.exists (io/file % (ns/path-for main))) source-path)))

(defn compilable-namespaces
  "Returns a seq of the namespaces that are compilable, regardless of whether
  their class files are present and up-to-date."
  [project]
  (let [nses (:aot project)
        nses (if (= :all nses)
               (mapcat ns/namespaces-in-dir (:source-path project))
               (find-namespaces-by-regex project nses))]
    (if (compile-main? project)
      (conj nses (:main project))
      nses)))

(defn stale-namespaces
  "Return a seq of namespaces that are both compilable and that have missing or
  out-of-date class files."
  [project]
  (filter
   (fn [n]
     (let [clj-path (ns/path-for n)
           class-file (io/file (:compile-path project)
                               (.replace clj-path "\\.clj" "__init.class"))]
       (or (not (.exists class-file))
           (> (.lastModified (io/file (:source-path project) clj-path))
              (.lastModified class-file)))))
   (compilable-namespaces project)))

 ;; .class file cleanup

;; (defn- has-source-package?
;;   "Test if the class file's package exists as a directory in source-path."
;;   [project f source-path]
;;   (and source-path
;;        (let [[[parent] [_ _ proxy-mod-parent]]
;;              (->> f, (iterate #(.getParentFile %)),
;;                   (take-while identity), rest,
;;                   (split-with #(not (re-find #"^proxy\$" (.getName %)))))]
;;          (.isDirectory (io/file (.replace (.getPath (or proxy-mod-parent parent))
;;                                           (:compile-path project)
;;                                           source-path))))))

;; (defn- class-in-project? [project f]
;;   (or (has-source-package? project f (:source-path project))
;;       (has-source-package? project f (:java-source-path project))
;;       (.exists (io/file (str (.replace (.getParent f)
;;                                        (:compile-path project)
;;                                        (:source-path project)) ".clj")))))

;; (defn- relative-path [project f]
;;   (let [root-length (if (= \/ (last (:compile-path project)))
;;                       (count (:compile-path project))
;;                       (inc (count (:compile-path project))))]
;;     (subs (.getAbsolutePath f) root-length)))

;; (defn- blacklisted-class? [project f]
;;   ;; true indicates all non-project classes are blacklisted
;;   (or (true? (:clean-non-project-classes project))
;;       (some #(re-find % (relative-path project f))
;;             (:clean-non-project-classes project))))

;; (defn- whitelisted-class? [project f]
;;   (or (class-in-project? project f)
;;       (and (:class-file-whitelist project)
;;            (re-find (:class-file-whitelist project)
;;                     (relative-path project f)))))

;; (defn clean-non-project-classes [project]
;;   (when (:clean-non-project-classes project)
;;     (doseq [f (file-seq (io/file (:compile-path project)))
;;             :when (and (.isFile f)
;;                        (not (whitelisted-class? project f))
;;                        (blacklisted-class? project f))]
;;       (.delete f))))

 ;; actual task

(defn- status [code msg]
  (binding [*out* (if (zero? code) *out* *err*)]
    (println msg))
  code)

(def ^:private success (partial status 0))
(def ^:private failure (partial status 1))

(defn compile
  "Compile Clojure source into .class files.

Uses the namespaces specified under :aot in project.clj or those given
as command-line arguments."
  ([project]
     (if (seq (compilable-namespaces project))
       (if-let [namespaces (seq (stale-namespaces project))]
         (try
           (let [form `(doseq [namespace# '~namespaces]
                         (println "Compiling" namespace#)
                         (clojure.core/compile namespace#))]
             (.mkdirs (io/file (:compile-path project)))
             ;; TODO: should eval-in-project be allowed to return non-integers?
             (if (zero? (eval/eval-in-project project form))
               (success "Compilation succeeded.")
               (failure "Compilation failed.")))
           #_(finally (clean-non-project-classes project)))
         (success "All namespaces already :aot compiled."))
       0))
  ([project & namespaces]
     (compile (assoc project :aot (if (= namespaces [":all"])
                                    :all
                                    (map symbol namespaces))))))
