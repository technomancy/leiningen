(ns leiningen.help
  "Display a list of tasks or help for a given task."
  (:use [clojure.contrib.find-namespaces :only [find-namespaces-on-classpath]]))

(def tasks (filter #(re-find #"^leiningen\.(?!core)" (name %))
                      (find-namespaces-on-classpath)))

;; affected by clojure ticket #130: bug of AOT'd namespaces losing metadata
(defn help-for [task]
  (let [task-ns (symbol (str "leiningen." task))
        _ (require task-ns)
        task (ns-resolve task-ns (symbol task))]
    (println (or (:doc (meta task))
                 (:doc (meta (find-ns task-ns)))))))

(defn help-summary-for [task-ns]
  (require task-ns)
  (let [task-name (last (.split (name task-ns) "\\."))]
    (println task-name (apply str (repeat (- 7 (count task-name)) " "))
             "-" (:doc (meta (find-ns task-ns))))))

(defn help [project & [task]]
  (if task
    (help-for task)
    (do (println "Leiningen is a build tool for Clojure.\n")
        (println "Several tasks are available:")
        (doseq [task-ns tasks]
          (help-summary-for task-ns))
        (println "\nRun lein help $TASK for details.")
        (println "See http://github.com/technomancy/leiningen as well."))))
