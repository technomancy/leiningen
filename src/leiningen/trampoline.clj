(ns leiningen.trampoline
  (:refer-clojure :exclude [trampoline])
  (:use [leiningen.core :only [apply-task task-not-found abort]]
        [leiningen.compile :only [get-readable-form prep eval-in-project]]
        [leiningen.classpath :only [get-classpath-string]])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [leiningen.util.paths :as paths]))

(def *trampoline?* false)

(defn get-jvm-opts [project]
  (let [legacy-native (paths/legacy-native-path project)]
    (filter identity [(if (not (empty? (System/getenv "JVM_OPTS")))
                        (System/getenv "JVM_OPTS"))
                      (if (:debug project)
                        "-Dclojure.debug=true")
                      (if (.exists (io/file (:native-path project)))
                        (str "-Djava.library.path=" (:native-path project))
                        (if (.exists (io/file legacy-native))
                          (str "-Djava.library.path=" legacy-native)))])))

(defn win-batch? []
  (.endsWith (System/getProperty "leiningen.trampoline-file") ".bat"))

(defn escape [form-string]
  (if (win-batch?)
    form-string
    (format "\"%s\"" (.replaceAll form-string "\"" "\\\\\""))))

(defn quote-path [string]
  (format "\"%s\"" (if (win-batch?) string (.replace string "\\" "\\\\"))))

(defn command-string [project java-cmd jvm-opts [form init]]
  (string/join " " [(quote-path java-cmd)
                    "-cp" (quote-path (get-classpath-string project))
                    (string/join " " (map quote-path jvm-opts))
                    "clojure.main" "-e"
                    (escape (get-readable-form (win-batch?) project form init))]))

(defn write-trampoline [command]
  (spit (System/getProperty "leiningen.trampoline-file") command))

(defn trampoline
  "Run a task without nesting the project's JVM inside Leiningen's.

Calculates what needs to run in the project's process for the provided
task and runs it after Leiningen's own JVM process has exited rather
than as a subprocess of Leiningen's project.

Use this to save memory or to work around things like Ant's stdin
issues. Not compatible with chaining.

ALPHA: subject to change without warning."
  [project task-name & args]
  (let [java-cmd (format "%s/bin/java" (System/getProperty "java.home"))
        jvm-opts (get-jvm-opts project)
        eval-args (atom nil)]
    (when (:eval-in-leiningen project)
      (println "Warning: trampoline has no effect with :eval-in-leiningen."))
    (binding [*trampoline?* true
              eval-in-project (fn [project form & [_ _ init]]
                                (prep project true)
                                (reset! eval-args [form init]) 0)]
      (apply-task task-name project args task-not-found))
    (if @eval-args
      (write-trampoline (command-string project java-cmd jvm-opts @eval-args))
      (abort task-name "is not trampolineable."))))
