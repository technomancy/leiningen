(ns leiningen.help
  "Display a list of tasks or help for a given task."
  (:use [leiningen.util.ns :only [namespaces-matching]])
  (:require [clojure.string :as string]))

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
    (format (str "%1s" (apply str (repeat padding " ")) "%2s")
      command
      (formatted-docstring command docstring padding))))

(defn- get-subtasks-and-docstrings-for [task]
  (let [task-ns (symbol (str "leiningen." task))
        task (ns-resolve task-ns (symbol task))]
    (into {}
      (map
        (fn [subtask]
          (let [m (meta subtask)]
            [(str (:name m)) (:doc m)]))
        (:subtasks (meta task))))))

(defn subtask-help-for
  [task-name]
  (let [subtasks (get-subtasks-and-docstrings-for task-name)]
    (if (empty? subtasks)
      nil
      (let [longest-key-length (apply max (map count (keys subtasks)))
            task-ns (doto (symbol (str "leiningen." task-name)) require)
            task (ns-resolve task-ns (symbol task-name))
            help-fn (ns-resolve task-ns 'help)]
          (string/join
            "\n"
            (concat ["\n\nSubtasks available:"]
                    (for [[subtask doc] subtasks]
                      (formatted-help subtask doc longest-key-length))))))))

(defn help-for
  "Help for a task is stored in its docstring, or if that's not present
  in its namespace."
  [task-name]
  (let [task-ns (doto (symbol (str "leiningen." task-name)) require)
        task (ns-resolve task-ns (symbol task-name))
        help-fn (ns-resolve task-ns 'help)]
    (str "Arguments: " (pr-str (get-arglists task)) "\n"
         (or (and help-fn (help-fn))
             (:doc (meta task))
             (:doc (meta (find-ns task-ns))))
         (subtask-help-for task-name))))

;; affected by clojure ticket #130: bug of AOT'd namespaces losing metadata
(defn help-summary-for [task-ns]
  (require task-ns)
  (let [task-name (last (.split (name task-ns) "\\."))]
    (str task-name (apply str (repeat (- 8 (count task-name)) " "))
         " - " (:doc (meta (find-ns task-ns))))))

(defn help
  "Display a list of tasks or help for a given task."
  ([task] (println (help-for task)))
  ([]
     (println "Leiningen is a build tool for Clojure.\n")
     (println "Several tasks are available:")
     (doseq [task-ns tasks]
       ;; (println (help-summary-for task-ns))
       (println " " (last (.split (name task-ns) "\\."))))
     (println "\nRun lein help $TASK for details.")
     (println "See http://github.com/technomancy/leiningen as well.")))
