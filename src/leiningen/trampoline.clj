(ns leiningen.trampoline
  "Run a task without nesting the project's JVM inside Leiningen's."
  (:refer-clojure :exclude [trampoline])
  (:require [clojure.string :as string]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]
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
  (if (win-batch?)
    (format "\"%s\"" arg)
    (format "'%s'" arg)))

(defn trampoline-command-string [project forms profiles]
  ;; each form is (do init & body)
  (let [forms (map rest forms) ;; strip off do
        inits (map first forms)
        rests (mapcat rest forms)
        command (eval/shell-command project (concat '(do) inits rests))]
    (string/join " " (map quote-arg command))))

(defn write-trampoline [project forms profiles]
  (let [command (trampoline-command-string project forms profiles)
        trampoline (trampoline-file)]
    (main/debug "Trampoline command:" command)
    (utils/mkdirs (.getParentFile (io/file trampoline)))
    (spit trampoline command)))

(defn ^:higher-order trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates the Clojure code to run in the project's process for the
given task and allows Leiningen's own JVM process to exit before
running it rather than launching a subprocess of Leiningen's JVM.

Use this to save memory or to work around stdin issues.

Note that this can be unpredictable on account of collapsing all
eval-in-project calls into one run. For example, tasks chained
together under different profiles end up all running together."

  [project task-name & args]
  (when (= :leiningen (:eval-in project))
    (main/info "Warning: trampoline has no effect with :eval-in-leiningen."))
  (binding [*trampoline?* true]
    (main/apply-task (main/lookup-alias task-name project)
                     (-> (assoc project :eval-in :trampoline)
                         (vary-meta update-in [:without-profiles] assoc
                                    :eval-in :trampoline))
                     args))
  (if (seq @eval/trampoline-forms)
    (write-trampoline @eval/trampoline-project
                      @eval/trampoline-forms
                      @eval/trampoline-profiles)
    (main/abort task-name "did not run any project code for trampolining.")))
