(ns leiningen.vcs
  (:require [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]))

;; TODO: make pom task use this ns by adding a few more methods

(def supported-systems (atom [:git]))

(defn uses-vcs [project vcs]
  (let [vcs-dir (io/file (:root project) (str "." (name vcs)))]
    (and (.exists vcs-dir) vcs)))

(defn which-vcs [project & _]
  (some (partial uses-vcs project) @supported-systems))


;;; Methods

(defmulti push "Push to your remote repository." which-vcs :default :none)

(defmulti tag "Apply a version control tag." which-vcs)

(defmulti assert-committed "Abort if uncommitted changes exist." which-vcs)


;;; Git

(defmethod push :git [project]
  (binding [eval/*dir* (:root project)]
    (eval/sh "git" "push")
    (eval/sh "git" "push" "--tags")))

(defmethod tag :git [project version]
  (binding [eval/*dir* (:root project)]
    (eval/sh "git" "tag" "-s" version "-m" (str "Release " version))))

(defmethod assert-committed :git [project]
  (binding [eval/*dir* (:root project)]
    (when-not (re-find #"working directory clean" (with-out-str
                                                    (eval/sh "git" "status")))
      (main/abort "Uncommitted changes in" (:root project) "directory."))))



(defn- not-found [subtask]
  (partial #'main/task-not-found (str "vcs " subtask)))

(defn ^{:subtasks [#'push #'tag]} vcs
  "Interact with the version control system."
  [project subtask & args]
  (let [subtasks (:subtasks (meta #'vcs) {})
        [subtask-var] (filter #(= subtask (name (:name (meta %)))) subtasks)]
    (apply (or subtask-var (not-found subtask)) project args)))
