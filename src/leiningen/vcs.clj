(ns leiningen.vcs
  "Interact with the version control system."
  (:require [clojure.java.io :as io]
            [bultitude.core :as b]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]))

;; TODO: make pom task use this ns by adding a few more methods

(def supported-systems (atom [:git]))

(defn uses-vcs [project vcs]
  (let [vcs-dir (io/file (:root project)
                         (get-in project [:scm :dir] "")
                         (str "." (name vcs)))]
    (and (.exists vcs-dir) vcs)))

(defn which-vcs [project & _]
  (or (:vcs project) (some (partial uses-vcs project) @supported-systems)))

(defn parse-tag-args [args]
  (loop [parsed-args {:sign? true :annotate? true}
         args args]
    (case (first args)
      ("--sign" "-s") (recur (assoc parsed-args :sign? true) (rest args))
      "--no-sign" (recur (assoc parsed-args :sign? false) (rest args))
      "--annotate" (recur (assoc parsed-args :annotate? true) (rest args))
      "--no-annotate" (recur (assoc parsed-args :annotate? false) (rest args))
      nil parsed-args                                       ;; We're finished and can exit
      (recur (assoc parsed-args :prefix (first args)) (rest args)))))

(def default-commit-message "Version %s")

;;; Methods

(defmulti push "Push to your remote repository."
  which-vcs :default :none)

(defmulti commit
  "Commit changes to current repository. Takes an optional format
  string for the commit message that will be provided the version."
  which-vcs :default :none)

(defmulti tag
  "Apply a version control tag. Takes an optional tag prefix.
  Pass --no-sign option to skip signing, or --no-annotate to create
  non-release tags."
  which-vcs :default :none)

(defmulti assert-committed "Abort if uncommitted changes exist."
  which-vcs :default :none)


;;; VCS not found

(defn- unknown-vcs [task]
  (binding [*out* *err*]
    (println (str "VCS not found for 'vcs " task "'"))
    (println "Maybe add :scm {:dir \"..\"} to project.clj?"))
  (System/exit 1))

(defmethod push :none [project & [args]] (unknown-vcs "push"))

(defmethod commit :none [project & _] (unknown-vcs "commit"))

(defmethod tag :none [project] (unknown-vcs "tag"))

(defmethod assert-committed :none [project] (unknown-vcs "assert-committed"))


;;; Git

(defmethod push :git [project & args]
  (binding [eval/*dir* (:root project)]
    (apply eval/sh-with-exit-code "Couldn't push to the remote" "git" "push" "--follow-tags" args)))

(defmethod commit :git
  ([project]
   (commit project default-commit-message))
  ([project message-template]
   (binding [eval/*dir* (:root project)]
     (let [message (format message-template (:version project))]
       (eval/sh-with-exit-code "Couldn't commit" "git" "commit" "-a" "-m" message)))))

(defmethod tag :git [{:keys [root version]} & args]
  (binding [eval/*dir* root]
    (let [{:keys [annotate? sign? prefix]} (parse-tag-args args)
          tag (if prefix
                (str prefix version)
                version)
          cmd (->> ["git" "tag" (if sign? "--sign") tag (if annotate? "-a") "-m" (str "Release " version)]
                   (filter some?))]
      (apply eval/sh-with-exit-code "Couldn't tag" cmd))))

(defmethod assert-committed :git [project]
  (binding [eval/*dir* (:root project)]
    (when (re-find #"Changes (not staged for commit|to be committed)"
                   (utils/with-system-out-str (eval/sh-with-exit-code "Couldn't get status" "git" "status")))
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
