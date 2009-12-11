(ns leiningen.help
  "Display a list of tasks or help for a given task."
  (:use [clojure.contrib.find-namespaces :only [find-namespaces-on-classpath]]))

(def tasks (set (filter #(re-find #"^leiningen\.(?!core)" (name %))
                        (find-namespaces-on-classpath))))

(defn help-for
  "Help for a task is stored in its docstring, or if that's not present
  in its namespace."
  [task]
  (let [task-ns (symbol (str "leiningen." task))
        _ (require task-ns)
        task (ns-resolve task-ns (symbol task))]
    (or (:doc (meta task))
        (:doc (meta (find-ns task-ns))))))

;; affected by clojure ticket #130: bug of AOT'd namespaces losing metadata
(defn help-summary-for [task-ns]
  (require task-ns)
  (let [task-name (last (.split (name task-ns) "\\."))]
    (str task-name (apply str (repeat (- 8 (count task-name)) " "))
         " - " (:doc (meta (find-ns task-ns))))))

(defn help
  ([task] (println (help-for task)))
  ([]
     (println "Leiningen is a build tool for Clojure.\n")
     (println "Several tasks are available:")
     (doseq [task-ns tasks]
       ;; (println (help-summary-for task-ns))
       (println " " (last (.split (name task-ns) "\\."))))
     (println "\nRun lein help $TASK for details.")
     (println "See http://github.com/technomancy/leiningen as well.")))
