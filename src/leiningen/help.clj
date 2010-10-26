(ns leiningen.help
  "Display a list of tasks or help for a given task."
  (:use [leiningen.util.ns :only [namespaces-matching]]))

(def tasks (->> (namespaces-matching "leiningen")
                (filter #(re-find #"^leiningen\.(?!core|util)[^\.]+$" (name %)))
                (distinct)
                (sort)))

(defn get-arglists [task]
  (for [args (or (:help-arglists (meta task)) (:arglists (meta task)))]
    (vec (remove #(= 'project %) args))))

(defn help-for
  "Help for a task is stored in its docstring, or if that's not present
  in its namespace."
  [task]
  (let [task-ns (doto (symbol (str "leiningen." task)) require)
        task (ns-resolve task-ns (symbol task))
        help-fn (ns-resolve task-ns 'help)]
    (str "Arguments: " (pr-str (get-arglists task)) "\n"
         (or (and help-fn (help-fn))
             (:doc (meta task))
             (:doc (meta (find-ns task-ns)))))))

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
