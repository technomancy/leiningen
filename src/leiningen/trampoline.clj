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

(defn trampoline-command-string [command]
  (string/join " " (if (win-batch?)
                     (map quote-arg command)
                     (conj (vec (map quote-arg (butlast command)))
                           (with-out-str
                             (prn (last command)))))))

(defn write-trampoline [command]
  (main/debug "Trampoline command:"
              (trampoline-command-string command))
  (spit (System/getProperty "leiningen.trampoline-file")
        (trampoline-command-string command)))

(defn ^:higher-order trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates what needs to run in the project's process for the provided
task and runs it after Leiningen's own JVM process has exited rather
than as a subprocess of Leiningen's project.

Use this to save memory or to work around things like stdin issues.
Not compatible with tasks like do that call eval-in-project multiple times."
  [project task-name & args]
  ;; TODO: allow trampoline calls to chain with do
  (let [command (promise)]
    (when (:eval-in-leiningen project)
      (main/info "Warning: trampoline has no effect with :eval-in-leiningen."))
    (binding [*trampoline?* true]
      (main/apply-task (main/lookup-alias task-name project)
                       (with-meta (assoc project :eval-in :trampoline)
                         {:trampoline-promise command}) args))
    (if (realized? command)
      (write-trampoline @command)
      (main/abort task-name "did not run any project code for trampolining."))))
