;; This is just a one-off tool to classify/summarize issues programmatically.

(try (require 'tentacles.issues)
     (catch java.io.FileNotFoundException _
       (cemerick.pomegranate/add-dependencies
        :repositories [["clojars" {:url "https://clojars.org/repo/"}]]
        :coordinates '[[tentacles "0.2.7"]])
       (require 'tentacles.issues)))

(defn labeled? [label issue] (some #(= (:name %) label) (:labels issue)))
(def low-priority? #{1566 1544 1319 1363 1155})
(def order ["2.4.3" "other" "Enhancement" "docs" "low" "3.0.0"])

(defn categorize [i]
  (cond (labeled? "Windows" i) nil
        (:title (:milestone i)) (:title (:milestone i))
        (labeled? "Enhancement" i) "Enhancement"
        (labeled? "docs" i) "docs"
        (low-priority? (:number i)) "low"
        :else "other"))

(defn report []
  (doseq [[category issues] (->> (tentacles.issues/issues
                                  "technomancy" "leiningen")
                                 (group-by categorize)
                                 (sort-by #(.indexOf order (key %))))
          :when category]
    (println "\n#" category)
    (doseq [i issues]
      (println (:number i) "-" (:title i)))))
