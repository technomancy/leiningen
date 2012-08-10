(ns leiningen.trampoline
  (:refer-clojure :exclude [trampoline])
  (:require [clojure.string :as string]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [clojure.pprint :as pprint]))

(def ^:dynamic *trampoline?* false)

(defn- win-batch? []
  (.endsWith (System/getProperty "leiningen.trampoline-file") ".bat"))

(defn- quote-arg [arg]
  (format "\"%s\"" arg))

(defn trampoline-command-string [project forms]
  ;; each form is (do init & body)
  (let [forms (map rest forms) ;; strip off do
        inits (map first forms)
        rests (mapcat rest forms)
        ;; TODO: tasks that assoc :dependencies in will not be honored here
        command (eval/shell-command project (concat '(do) inits rests))]
    (string/join " " (if (win-batch?)
                       (map quote-arg command)
                       (conj (vec (map quote-arg (butlast command)))
                             (with-out-str
                               (prn (last command))))))))

(defn write-trampoline [project forms]
  (let [command (trampoline-command-string project forms)]
    (main/debug "Trampoline command:" command)
    (spit (System/getProperty "leiningen.trampoline-file") command)))

(defn ^:higher-order trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates what needs to run in the project's process for the provided
task and runs it after Leiningen's own JVM process has exited rather
than as a subprocess of Leiningen's project.

Use this to save memory or to work around stdin issues."
  [project task-name & args]
  ;; TODO: allow trampoline calls to chain with do (does this already work?)
  (let [forms (atom [])]
    (when (:eval-in-leiningen project)
      (main/info "Warning: trampoline has no effect with :eval-in-leiningen."))
    (binding [*trampoline?* true]
      (main/apply-task (main/lookup-alias task-name project)
                       (with-meta (assoc project :eval-in :trampoline)
                         {:trampoline-forms forms}) args))
    (if (seq @forms)
      (write-trampoline project @forms)
      (main/abort task-name "did not run any project code for trampolining."))))
