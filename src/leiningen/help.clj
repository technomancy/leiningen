(ns leiningen.help
  "Display a list of tasks or help for a given task."
  (:use [leiningen.util.ns :only [namespaces-matching]])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(def tasks (->> (namespaces-matching "leiningen")
                (filter #(re-find #"^leiningen\.(?!core|util)[^\.]+$" (name %)))
                (distinct)
                (sort)))

(defn get-arglists [task]
  (for [args (or (:help-arglists (meta task)) (:arglists (meta task)))]
    (vec (remove #(= 'project %) args))))

(def help-padding 3)

(defn- formatted-docstring [command docstring padding]
  (apply str
    (replace
      {\newline
       (apply str
         (cons \newline (repeat (+ padding (count command)) \space)))}
      docstring)))

(defn- formatted-help [command docstring longest-key-length]
  (let [padding (+ longest-key-length help-padding (- (count command)))]
    (format (str "%1s" (apply str (repeat padding \space)) "%2s")
      command
      (formatted-docstring command docstring padding))))

(defn- get-subtasks-and-docstrings-for [task]
  (into {}
    (map (fn [subtask]
           (let [m (meta subtask)]
             [(str (:name m)) (:doc m)]))
         (:subtasks (meta task)))))

(defn subtask-help-for
  [task-ns task]
  (let [subtasks (get-subtasks-and-docstrings-for task)]
    (if (empty? subtasks)
      nil
      (let [longest-key-length (apply max (map count (keys subtasks)))
            help-fn (ns-resolve task-ns 'help)]
        (string/join "\n"
          (concat ["\n\nSubtasks available:"]
                  (for [[subtask doc] subtasks]
                    (formatted-help subtask doc longest-key-length))))))))

(defn- resolve-task [task-name]
  (let [task-ns (doto (symbol (str "leiningen." task-name)) require)
        task (ns-resolve task-ns (symbol task-name))]
    [task-ns task]))

(defn static-help [name]
  (when-let [reader (io/resource (format "leiningen/help/%s" name))]
    (slurp reader)))

(defn help-for
  "Help for a task is stored in its docstring, or if that's not present
  in its namespace."
  [task-name]
  (let [[task-ns task] (resolve-task task-name)
        help-fn (ns-resolve task-ns 'help)]
    (str (or (and (not= task-ns 'leiningen.help) help-fn (help-fn))
             (:doc (meta task))
             (:doc (meta (find-ns task-ns))))
         (subtask-help-for task-ns task)
         (when (some seq (get-arglists task))
           (str "\n\nArguments: " (pr-str (get-arglists task)))))))

;; affected by clojure ticket #130: bug of AOT'd namespaces losing metadata
(defn help-summary-for [task-ns]
  (try (let [task-name (last (.split (name task-ns) "\\."))
             ns-summary (:doc (meta (find-ns (doto task-ns require))))
             first-line (first (.split (help-for task-name) "\n"))]
         ;; Use first line of task docstring if ns metadata isn't present
         (str task-name (apply str (repeat (- 12 (count task-name)) " "))
              (or ns-summary first-line)))
       (catch Throwable e
         (binding [*out* *err*]
           (str task-ns "  Problem loading: " (.getMessage e))))))

(defn help
  "Display a list of tasks or help for a given task.

Also provides readme, tutorial, news, sample, deploying and copying documentation."
  ([task] (println (or (static-help task) (help-for task))))
  ([]
     (println "Leiningen is a tool for working with Clojure projects.\n")
     (println "Several tasks are available:")
     (doseq [task-ns tasks]
       (println (help-summary-for task-ns)))
     (println "\nRun lein help $TASK for details.")
     (println "See also: readme, tutorial, copying, sample, deploying and news.")))
