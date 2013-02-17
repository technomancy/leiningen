(ns leiningen.trampoline
  (:refer-clojure :exclude [trampoline])
  (:require [clojure.string :as string]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.logger :as log]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(def ^:dynamic *trampoline?* false)

(defn- trampoline-file []
  (System/getenv "TRAMPOLINE_FILE"))

(defn- win-batch? []
  (if-let [t (trampoline-file)]
    (.endsWith t ".bat")))

(defn- quote-arg [arg]
  (format "\"%s\"" arg))

(defn trampoline-command-string [project forms profiles]
  ;; each form is (do init & body)
  (let [forms (map rest forms) ;; strip off do
        inits (map first forms)
        rests (mapcat rest forms)
        ;; This won't pick up :jvm-args that come from profiles, but it
        ;; at least gets us :dependencies.
        project (project/merge-profiles project profiles)
        command (eval/shell-command project (concat '(do) inits rests))]
    (string/join " " (if (win-batch?)
                       (map quote-arg command)
                       (conj (vec (map quote-arg (butlast command)))
                             (with-out-str
                               (prn (last command))))))))

(defn write-trampoline [project forms profiles]
  (let [command (trampoline-command-string project forms profiles)
        trampoline (trampoline-file)]
    (log/debug "Trampoline command:" command)
    (.mkdirs (.getParentFile (io/file trampoline)))
    (spit trampoline command)))

(defn ^:higher-order trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates the Clojure code to run in the project's process for the
given task and allows Leiningen's own JVM process to exit before
running it rather than launching a subprocess of Leiningen's JVM.

Use this to save memory or to work around stdin issues."
  [project task-name & args]
  (when (= :leiningen (:eval-in project))
    (log/info "trampoline has no effect with :eval-in-leiningen."))
  (binding [*trampoline?* true]
    (main/apply-task (main/lookup-alias task-name project)
                     (-> (assoc project :eval-in :trampoline)
                         (vary-meta update-in [:without-profiles] assoc
                                    :eval-in :trampoline))
                     args))
  (if (seq @eval/trampoline-forms)
    (write-trampoline project @eval/trampoline-forms @eval/trampoline-profiles)
    (log/abort task-name "did not run any project code for trampolining.")))
