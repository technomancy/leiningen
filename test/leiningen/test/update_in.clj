(ns leiningen.test.update-in
  (:refer-clojure :exclude [update-in])
  (:use clojure.test leiningen.update-in))

(defn- prj-map [p] (with-meta p {:without-profiles p}))

(deftest test-update-in
  (doseq
   [[in-args task-form]
    (->> [[(prj-map {:version "1.0.0"})
           ":" "assoc" ":version" "\"2.0.0\"" "--" "jar"]
          ["jar" (prj-map {:version "2.0.0"})]

          [(prj-map {:repl-options {:port 1}})
           ":repl-options:port" "inc" "--" "repl" ":headless"]
          ["repl" (prj-map {:repl-options {:port 2}}) ":headless"]

          [(prj-map {:dependencies [['clojure.core (clojure-version)]]})
           ":dependencies" "conj" "[slamhound \"1.1.3\"]" "--" "repl"]
          ["repl" (prj-map {:dependencies [['clojure.core (clojure-version)]
                                           ['slamhound "1.1.3"]]})]]
         (partition 2))]
    (let [[in-prj key-path f & args] in-args
          [keys-vec f f-args [task-name & task-args]]
          (parse-args key-path f args)
          out-prj (update-project in-prj keys-vec f f-args)]
      (is (= task-form (concat [task-name out-prj] task-args)))
      (is (= (meta (second task-form)) (meta out-prj))))))
