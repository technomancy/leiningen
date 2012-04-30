(ns leiningen.core.main
  (:require [leiningen.core.user :as user]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def aliases (atom {"--help" "help", "-h" "help", "-?" "help", "-v" "version"
                    "--version" "version", "überjar" "uberjar"
                    "cp" "classpath" "halp" "help"
                    "with-profiles" "with-profile"
                    "readme" ["help" "readme"]
                    "tutorial" ["help" "tutorial"]
                    "sample" ["help" "sample"]}))

(def ^:dynamic *debug* (System/getenv "DEBUG"))

(defn debug [& args]
  (when *debug* (apply println args)))

(def ^:dynamic *info* true)

(defn info [& args]
  (when *info* (apply println args)))

(def ^:dynamic *exit-process?*
  "Bind to false to suppress process termination." true)

(defn exit
  "Exit the process. Rebind *exit-process?* in order to suppress actual process
  exits for tools which may want to continue operating."
  ([code]
     (if *exit-process?*
       (do (shutdown-agents)
           (System/exit code))
       (throw (Exception. (str "Suppressed exit: " code)))))
  ([] (exit 0)))

(defn abort
  "Print msg to standard err and exit with a value of 1."
  [& msg]
  (binding [*out* *err*]
    (apply println msg)
    (exit 1)))

(defn ^:no-project-needed task-not-found [task & _]
  (abort (str task " is not a task. Use \"lein help\" to list all tasks.")))

;; TODO: got to be a cleaner way to do this, right?
(defn- drop-partial-args [pargs]
  #(for [[f & r] %
         :let [non-varargs (if (pos? (inc (.indexOf (or r []) '&)))
                             (min (count pargs) (.indexOf r '&))
                             (count pargs))]]
     (cons f (drop non-varargs r))))

(defn resolve-task
  ([task not-found]
     (let [[task & pargs] (if (coll? task) task [task])
           task-ns (symbol (str "leiningen." task))]
       (try
         (when-not (find-ns task-ns)
           (require task-ns))
         (if-let [task-var (ns-resolve task-ns (symbol task))]
           (with-meta
             (fn [project & args] (apply task-var project (concat pargs args)))
             (update-in (meta task-var) [:arglists] (drop-partial-args pargs)))
           (not-found task))
         (catch java.io.FileNotFoundException e
           (not-found task)))))
  ([task] (resolve-task task #'task-not-found)))

(defn ^:internal matching-arity? [task args]
  (some (fn [parameters]
          (and (if (= '& (last (butlast parameters)))
                 (>= (count args) (- (count parameters) 3))
                 (= (count parameters) (inc (count args))))
               parameters))
        (:arglists (meta task))))

(defn apply-task [task-name project args]
  (let [task (resolve-task task-name)]
    (when-not (or project (:no-project-needed (meta task)))
      (abort "Couldn't find project.clj, which is needed for" task-name))
    (when-not (matching-arity? task args)
      (abort "Wrong number of arguments to" task-name "task."
             "\nExpected" (rest (:arglists (meta task)))))
    (apply task project args)))

(defn ^:internal version-satisfies? [v1 v2]
  (let [v1 (map #(Integer. %) (re-seq #"\d+" (first (string/split v1 #"-" 2))))
        v2 (map #(Integer. %) (re-seq #"\d+" (first (string/split v2 #"-" 2))))]
    (loop [versions (map vector v1 v2)
           [seg1 seg2] (first versions)]
      (cond (empty? versions) true
            (= seg1 seg2) (recur (rest versions) (first (rest versions)))
            (> seg1 seg2) true
            (< seg1 seg2) false))))

(def ^:private min-version-warning
  "*** Warning: This project requires Leiningen %s, but you have %s ***

Get the latest verison of Leiningen at https://github.com/technomancy/leiningen
or by executing \"lein upgrade\". ")

(defn- verify-min-version
  [{:keys [min-lein-version]}]
  (when-not (version-satisfies? (System/getenv "LEIN_VERSION") min-lein-version)
    (info (format min-version-warning
                  min-lein-version (System/getenv "LEIN_VERSION")))))

(defn- conj-to-last [coll x]
  (update-in coll [(dec (count coll))] conj x))

(defn ^:internal group-args
  ([args] (reduce group-args [[]] args))
  ([groups arg]
     (if (.endsWith arg ",")
       (-> groups
           (conj-to-last (subs arg 0 (dec (count arg))))
           (conj []))
       (conj-to-last groups arg))))

(defn -main
  "Run a task or comma-separated list of tasks."
  [& args]
  (user/init)
  (let [project (if (.exists (io/file "project.clj"))
                  (project/init-project (project/read)))]
    (when (:min-lein-version project)
      (verify-min-version project))
    (when-let [{:keys [host port]} (classpath/get-proxy-settings)]
      (System/setProperty "http.proxyHost" host)
      (System/setProperty "http.proxyPort" (str port)))
    (when-not project
      (project/load-plugins (project/merge-profiles {} [:user :default])))
    (doseq [[task-name & args] (group-args args)
            :let [task-name (or (@aliases task-name)
                                (get (:aliases project) task-name)
                                task-name "help")]]
      (try (apply-task task-name project args)
           (catch Exception e
             (when-let [[_ code] (and (.getMessage e)
                                      (re-find #"Process exited with (\d+)"
                                               (.getMessage e)))]
               (exit (Integer. code)))
             (when-not (re-find #"Suppressed exit:" (or (.getMessage e) ""))
               (.printStackTrace e))
             (exit 1)))))
  (exit 0))