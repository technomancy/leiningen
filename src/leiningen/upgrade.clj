(ns leiningen.upgrade
  "Upgrade Leiningen to specified version or latest stable."
  (:require [leiningen.core.main :as main]))

;; This file is only a placeholder. The real upgrade
;; implementation can be found in the 'lein' script.

(defn ^:no-project-needed upgrade
  "Upgrade Leiningen to specified version or latest stable."
  [project & args]
  (main/abort "Upgrade is either disabled, or you have tried to call it from a"
              "higher order\ntask. If you've installed lein through a package"
              "manager, upgrade lein through\nthe package manager's upgrade"
              "commands."))
