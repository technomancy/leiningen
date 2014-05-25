(ns leiningen.vcs
  "Interact with the version control system."
  (:require [clojure.java.io :as io]
            [bultitude.core :as b]
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

(defmulti commit "Save indexed changes to current repository." which-vcs :default :none)

(defmulti tag "Apply a version control tag." which-vcs)

(defmulti assert-committed "Abort if uncommitted changes exist." which-vcs)


;;; Git

(defmethod push :git [project]
  (binding [eval/*dir* (:root project)]
    (eval/sh "git" "push")
    (eval/sh "git" "push" "--tags")))

(defmethod commit :git [project]
  (binding [eval/*dir* (:root project)]
    (eval/sh "git" "add" "-A")
    (eval/sh "git" "commit" "-m" (str "Version " (:version project)))))

(defmethod tag :git [project]
  (binding [eval/*dir* (:root project)]
    (let [version (:version project)]
      (eval/sh "git" "tag" "-s" version "-m" (str "Release " version)))))

(defmethod assert-committed :git [project]
  (binding [eval/*dir* (:root project)]
    (when (re-find #"Changes (not staged for commit|to be committed)"
                   (with-out-str (eval/sh "git" "status")))
       (main/abort "Uncommitted changes in" (:root project) "directory."))))



(defn- not-found [subtask]
  (partial #'main/task-not-found (str "vcs " subtask)))

(defn- load-methods []
  (doseq [n (b/namespaces-on-classpath :prefix "leiningen.vcs.")]
    (swap! supported-systems conj (keyword (last (.split (name n) "\\."))))
    (require n)))

(defn ^{:subtasks [#'push #'commit #'tag #'assert-committed]} vcs
  "Interact with the version control system."
  [project subtask & args]
  (load-methods)
  (let [subtasks (:subtasks (meta #'vcs) {})
        [subtask-var] (filter #(= subtask (name (:name (meta %)))) subtasks)]
    (apply (or subtask-var (not-found subtask)) project args)))
