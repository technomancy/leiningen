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
        (println (format "[%s %s]" dep (result "latestVersion")))))))

(defn search-clojars [query]
  (println "Searching Clojars...")
  (let [url (str "https://clojars.org/search?format=json&q=" query)]
    (doseq [result (get (json/decode (:body (http/get url))) "results")]
      (let [dep (if (= (result "jar_name") (result "group_name"))
                  (result "jar_name")
                  (str (result "group_name") "/" (result "jar_name")))]
        (println (format "[%s %s]" dep (result "version")))
        (when-let [desc (and (not= (string/trim (result "description" "")) "")
                             (result "description"))]
          (println " " (string/trim (first (string/split desc #"\n")))))))))

(defn ^:no-project-needed search
  "Search Maven Central and Clojars for published artifacts.

The query is evaluated as a lucene search. You can search for simple string
matches or do more advanced queries such as this:

  $ lein search clojure
  $ lein search description:crawl
  $ lein search group:clojurewerkz
  $ lein search \"id:clojure version:1.6\"
  $ lein search \"Riak client\"
"
  [project query]
  (let [project (or project (project/make {}))
        repos (into {} (:repositories project))]
    (when (repos "central")
      (search-central query))
    (when (repos "clojars")
      (search-clojars query))))
