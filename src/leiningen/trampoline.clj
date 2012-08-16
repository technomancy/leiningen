(ns leiningen.trampoline
  (:refer-clojure :exclude [trampoline])
  (:require [clojure.string :as string]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [clojure.pprint :as pprint]))

(def ^:dynamic *trampoline?* false)

(defn- win-batch? []
  (.endsWith (System/getProperty "leiningen.trampoline-file") ".bat"))

(defn- quote-arg [arg]
  (format "\"%s\"" arg))

(defn trampoline-command-string [project forms deps]
  ;; each form is (do init & body)
  (let [forms (map rest forms) ;; strip off do
        inits (map first forms)
        rests (mapcat rest forms)
        ;; This won't pick up :jvm-args that come from profiles, but it
        ;; at least gets us :dependencies.
        project (project/normalize-deps (assoc project :dependencies
                                               (apply concat deps)))
        command (eval/shell-command project (concat '(do) inits rests))]
    (string/join " " (if (win-batch?)
                       (map quote-arg command)
                       (conj (vec (map quote-arg (butlast command)))
                             (with-out-str
                               (prn (last command))))))))

(defn write-trampoline [project forms deps]
  (let [command (trampoline-command-string project forms deps)]
    (main/debug "Trampoline command:" command)
    (spit (System/getProperty "leiningen.trampoline-file") command)))

(defn ^:higher-order trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates what needs to run in the project's process for the provided
task and runs it after Leiningen's own JVM process has exited rather
than as a subprocess of Leiningen's project.

Use this to save memory or to work around stdin issues."
  [project task-name & args]
  (let [forms (atom []), deps (atom [])]
    (when (:eval-in-leiningen project)
      (main/info "Warning: trampoline has no effect with :eval-in-leiningen."))
    (binding [*trampoline?* true]
      (main/apply-task (main/lookup-alias task-name project)
                       (-> (assoc project :eval-in :trampoline)
                           (vary-meta assoc
                                      :trampoline-forms forms
                                      :trampoline-deps deps)
                           (vary-meta update-in [:without-profiles] assoc
                                      :eval-in :trampoline))
                        args))
    (if (seq @forms)
      (write-trampoline project @forms @deps)
      (main/abort task-name "did not run any project code for trampolining."))))
