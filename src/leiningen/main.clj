(ns leiningen.main
  (:require [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def aliases (atom {"--help" "help", "-h" "help", "-?" "help", "-v" "version"
                    "--version" "version", "überjar" "uberjar"
                    "cp" "classpath"}))

(defn exit
  "Call System/exit. Defined as a function so that rebinding is possible."
  ([code]
     (shutdown-agents)
     (System/exit code))
  ([] (exit 0)))

(defn abort
  "Print msg to standard err and exit with a value of 1."
  [& msg]
  (binding [*out* *err*]
    (apply println msg)
    (exit 1)))

(defn task-not-found [& _]
  (abort "That's not a task. Use \"lein help\" to list all tasks."))

(defn resolve-task
  ([task not-found]
     (let [task-ns (symbol (str "leiningen." task))
           task (symbol task)]
       (try
         (when-not (find-ns task-ns)
           (require task-ns))
         (or (ns-resolve task-ns task)
             not-found)
         (catch java.io.FileNotFoundException e
           not-found))))
  ([task] (resolve-task task #'task-not-found)))

(defn matching-arity? [task project args]
  (some (fn [parameters]
          (and (if (= '& (last (butlast parameters)))
                 (>= (count args) (- (count parameters) 2))
                 (= (count parameters) (inc (count args))))
               parameters))
        (:arglists (meta task))))

(defn apply-task [task-name project args]
  (let [task (resolve-task task-name)]
    (when-not (or project (:no-project-needed (meta task)))
      (abort "Couldn't find project.clj, which is needed for" task-name))
    (when-not (matching-arity? task project args)
      (abort "Wrong number of arguments to" task-name "task."
             "\nExpected" (rest (:arglists (meta task)))))
    (apply task project args)))

(defn- version-satisfies? [v1 v2]
  (let [v1 (map #(Integer. %) (re-seq #"\d" (first (string/split v1 #"-" 2))))
        v2 (map #(Integer. %) (re-seq #"\d" (first (string/split v2 #"-" 2))))]
    (or (and (every? true? (map >= v1 v2))
             (>= (count v1) (count v2)))
        (every? true? (map > v1 v2)))))

(def ^:private min-version-warning
  "*** Warning: This project requires Leiningen %s, but you have %s ***

Get the latest verison of Leiningen at https://github.com/technomancy/leiningen
or by executing \"lein upgrade\". ")

(defn- verify-min-version
  [{:keys [min-lein-version]}]
  (when-not (version-satisfies? (System/getenv "LEIN_VERSION") min-lein-version)
    (println (format min-version-warning
                     min-lein-version (System/getenv "LEIN_VERSION")))))

(defn- conj-to-last [coll x]
  (update-in coll [(dec (count coll))] conj x))

(defn- group-args
  ([args] (reduce group-args [[]] args))
  ([groups arg]
     (if (.endsWith arg ",")
       (-> groups
           (conj-to-last (subs arg 0 (dec (count arg))))
           (conj []))
       (conj-to-last groups arg))))

(defn -main
  "Run a task or comma-separated list of tasks."
  ([task-name & args]
     (user/init)
     (let [task-name (or (@aliases task-name) task-name "help")
           project (if (.exists (io/file "project.clj")) (project/read))]
       (when (:min-lein-version project)
         (verify-min-version project))
       (apply-task task-name project args)))
  ([]
     (doseq [[task & args] (group-args *command-line-args*)
             :let [result (apply -main (or task "help") args)]]
       (when (and (integer? result) (pos? result))
         (exit result)))
     (exit 0)))