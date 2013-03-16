(ns leiningen.help
  "Display a list of tasks or help for a given task."
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [leiningen.core.main :as main]))

(def ^{:private true
       :doc "Width of task name column in list of tasks produced by help task."}
  task-name-column-width 20)

(defn- get-arglists [task]
  (for [args (or (:help-arglists (meta task)) (:arglists (meta task)))]
    (vec (remove #(= 'project %) args))))

(def ^:private help-padding 3)

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
                 [(str (:name m)) (first (.split (:doc m "") "\n"))]))
             (:subtasks (meta task)))))

(defn subtask-help-for
  [task-ns task]
  (if-let [subtasks (seq (get-subtasks-and-docstrings-for task))]
    (let [longest-key-length (apply max (map count (keys subtasks)))]
      (string/join "\n" (concat ["\n\nSubtasks available:"]
                                (for [[subtask doc] subtasks]
                                  (formatted-help subtask doc
                                                  longest-key-length))
                                [(str "\nRun `lein help " (:name (meta task))
                                      " $SUBTASK` for subtask details.")])))))

(defn- resolve-task [task-name]
  (try (let [task-ns (doto (symbol (str "leiningen." task-name)) require)
             task (ns-resolve task-ns (symbol task-name))]
         [task-ns task])
       (catch java.io.FileNotFoundException e
         [nil nil])))

(defn- resolve-subtask [task-name subtask-name]
  (let [[_ task] (resolve-task task-name)]
    (some #(if (= (symbol subtask-name) (:name (meta %))) %)
          (:subtasks (meta task)))))

(defn- static-help [name]
  (if-let [resource (io/resource (format "leiningen/help/%s" name))]
    (slurp resource)))

(defn help-for
  "Returns a string containing help for a task.
Looks for a function named 'help' in the subtask's namespace,
then a docstring on the task, then a docstring on the task ns."
  ([task-name]
     (let [[task-ns task] (resolve-task task-name)]
       (if task
         (let [help-fn (ns-resolve task-ns 'help)]
           (str (or (and (not= task-ns 'leiningen.help) help-fn (help-fn))
                    (:doc (meta task))
                    (:doc (meta (find-ns task-ns))))
                (subtask-help-for task-ns task)
                (if (some seq (get-arglists task))
                  (str "\n\nArguments: " (pr-str (get-arglists task))))))
         (format "Task: '%s' not found" task-name))))
  ([project task-name]
     (let [aliases (merge main/aliases (:aliases project))]
       (help-for (aliases task-name task-name)))))

(defn help-for-subtask
  "Returns a string containing help for a subtask.
Looks for a function named 'help-<subtask>' in the subtask's namespace,
using the subtask's docstring if the help function is not found."
  ([task-name subtask-name]
     (if-let [subtask (resolve-subtask task-name subtask-name)]
       (let [subtask-meta (meta subtask)
             help-fn (ns-resolve (:ns subtask-meta)
                                 (symbol (str "help-" subtask-name)))
             arglists (get-arglists subtask)]
         (str (or (and help-fn (help-fn)) (:doc subtask-meta))
              (if (some seq arglists)
                (str "\n\nArguments: " (pr-str arglists)))))
       (format "Subtask: '%s %s' not found" task-name subtask-name)))
  ([project task-name subtask-name]
     (let [aliases (merge main/aliases (:aliases project))]
       (help-for-subtask (aliases task-name task-name) subtask-name))))

(defn help-summary-for [task-ns]
  (try (let [task-name (last (.split (name task-ns) "\\."))
             ns-summary (:doc (meta (find-ns (doto task-ns require))))
             first-line (first (.split (help-for {} task-name) "\n"))]
         ;; Use first line of task docstring if ns metadata isn't present
         (str task-name (apply str (repeat (- task-name-column-width
                                              (count task-name)) " "))
              (or ns-summary first-line)))
       (catch Throwable e
         (binding [*out* *err*]
           (str task-ns "  Problem loading: " (.getMessage e))))))

(defn ^:no-project-needed ^:higher-order help
  "Display a list of tasks or help for a given task or subtask.

Also provides readme, faq, tutorial, news, sample, profiles,
deploying, mixed-source, templates, and copying info."
  ([project task subtask] (println (or (static-help (str task "-" subtask))
                                       (help-for-subtask project task subtask))))
  ([project task] (println (or (static-help task) (help-for project task))))
  ([project]
     (println "Leiningen is a tool for working with Clojure projects.\n")
     (println "Several tasks are available:")
     (doseq [task-ns (main/tasks)]
       (println (help-summary-for task-ns)))
     (println "\nRun `lein help $TASK` for details.")
     (println "\nGlobal Options:")
     (println "  -o             Run a task offline.")
     (println "  -U             Run a task after forcing update of snapshots.")
     (println "  -h, --help     Print this help.")
     (println "  -v, --version  Print Leiningen's version.")
     (when-let [aliases (:aliases project)]
       (println "\nAliases:")
       (doseq [[k v] aliases]
         (println (str k  " " v))))
     (println "\nSee also: readme, faq, tutorial, news, sample, profiles,"
              "deploying, gpg, mixed-source, templates, and copying.")))
