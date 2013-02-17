(ns leiningen.core.logger)

(def ^:dynamic *exit-process?*
  "Bind to false to suppress process termination." true)

(defn exit
  "Exit the process. Rebind *exit-process?* in order to suppress actual process
  exits for tools which may want to continue operating. Never call
  System/exit directly in Leiningen's own process."
  ([exit-code]
     (if *exit-process?*
       (do (shutdown-agents)
           (System/exit exit-code))
       (throw (ex-info "Suppressed exit" {:exit-code exit-code}))))
  ([] (exit 0)))

(def ^:dynamic *debug* (System/getenv "DEBUG"))

(defn debug
  "Print if *debug* (from DEBUG environment variable) is truthy."
  [& msg]
  (when *debug* 
    (print "DEBUG: ")
    (apply println msg)))

(def ^:dynamic *info* true)

(defn info
  "Print unless *info* has been rebound to false."
  [& msg]
  (when *info* 
    (print "INFO: ")
    (apply println msg)))

(defn warn
  "Print a warning message to standard err"
  [& msg]
  (binding [*out* *err*]
    (when (seq msg)
      (print "WARNING: ")
      (apply println msg))))

(defn abort
  "Print an error msg to standard err and exit with a value of 1.
  Will not directly exit under some circumstances; see *exit-process?*."
  [& msg]
  (binding [*out* *err*]
    (println 
      "Leiningen has encountered a critical error and will now exit.")
    (when (seq msg)
      (print "ERROR: ")
      (apply println msg))
    (exit 1)))