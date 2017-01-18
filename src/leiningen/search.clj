(ns leiningen.search
  "Search remote maven repositories for matching jars."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.user :as user]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(defn search-central [query]
  (println "Searching Central...")
  (let [url (str "http://search.maven.org/solrsearch/select?wt=json&q=" query)]
    (doseq [result (get-in (json/decode (:body (http/get url)))
                           ["response" "docs"])]
      (let [dep (if (= (result "a") (result "g"))
                  (result "a")
                  (str (result "g") "/" (result "a")))]
        (println (format "[%s \"%s\"]" dep (result "latestVersion")))))))

(defn search-clojars [query]
  (println "Searching Clojars...")
  (let [url (str "https://clojars.org/search?format=json&q=" query)]
    (doseq [result (get (json/decode (:body (http/get url))) "results")]
      (let [dep (if (= (result "jar_name") (result "group_name"))
                  (result "jar_name")
                  (str (result "group_name") "/" (result "jar_name")))]
        (println (format "[%s \"%s\"]" dep (result "version")))
        (when-let [desc (and (not= (string/trim (result "description" "")) "")
                             (result "description"))]
          (println " " (string/trim (first (string/split desc #"\n")))))))))

(defn ^:no-project-needed search
  "Search Maven Central and Clojars for published artifacts."
  [project query]
  (let [project (or project (project/make {}))
        repos (into {} (:repositories project))]
    (when (repos "central")
      (try (search-central query)
           (catch clojure.lang.ExceptionInfo e
             (if (= 400 (:status (ex-data e)))
               (println "Query syntax unsupported by Central.")
               (throw e)))))
    (when (repos "clojars")
      (try (search-clojars query)
           (catch clojure.lang.ExceptionInfo e
             (if (= 400 (:status (ex-data e)))
               (println "Query syntax unsupported by Clojars.")
               (throw e)))))))
