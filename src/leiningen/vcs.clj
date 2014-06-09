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
  (or (:vcs project) (some (partial uses-vcs project) @supported-systems)))


;;; Methods

(defmulti push "Push to your remote repository."
  which-vcs :default :none)

(defmulti commit "Commit changes to current repository."
  which-vcs :default :none)

(defmulti tag "Apply a version control tag. Takes an optional tag prefix."
  which-vcs :default :none)

(defmulti assert-committed "Abort if uncommitted changes exist."
  which-vcs :default :none)



;;; VCS not found

(defn- unknown-vcs [task]
  (binding [*out* *err*]
    (println (str "Unknown VCS detected for 'vcs " task "'")))
  (System/exit 1))

(defmethod push :none [project & [args]] (unknown-vcs "push"))

(defmethod commit :none [project] (unknown-vcs "commit"))

(defmethod tag :none [project] (unknown-vcs "tag"))

(defmethod assert-committed :none [project] (unknown-vcs "assert-committed"))

;;; Git

(defmethod push :git [project & args]
  (binding [eval/*dir* (:root project)]
    (apply eval/sh "git" "push" args)
    (apply eval/sh "git" "push" "--tags" args)))

(defmethod commit :git [project]
  (binding [eval/*dir* (:root project)]
    (eval/sh "git" "add" "-A")
    (eval/sh "git" "commit" "-m" (str "Version " (:version project)))))

(defmethod tag :git [{:keys [root version]} & [prefix]]
  (binding [eval/*dir* root]
    (let [tag (if prefix
                (str prefix "-" version)
                version)]
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
